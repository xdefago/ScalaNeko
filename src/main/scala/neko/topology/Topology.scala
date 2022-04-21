/*
 * Copyright 2017 Xavier Défago (Tokyo Institute of Technology)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package neko.topology

import neko._

import scalax.collection.Graph
import scalax.collection.GraphEdge.UnDiEdge
import scalax.collection.GraphPredef._


/**
 * Describes a network topology.
 *
 * It consists in a set of processes, and possibly an underlying graph.
 *
 */

trait Topology
{
  def underlying : Graph[Int,UnDiEdge]
  def processSet : Set[PID]

  def neighborsFor(process: PID): Option[Set[PID]]

  def contains(process: PID): Boolean
  def isConnected: Boolean
  def size: Int
  def numberOfEdges: Int

  def union    (that: Topology): Topology
  def diff     (that: Topology): Topology
  def intersect(that: Topology): Topology
}




object Topology
{
  def from(graph: Graph[Int,UnDiEdge]): Topology =
    new GraphTopology {
      val underlying = graph
    }



  def ringOf(range: Range): Graph[Int,UnDiEdge] =
    Graph.from(
      nodes = range,
      edges = for (i <- range; j = if (i < range.last) i + 1 else range.min) yield i ~ j
    )

  def cliqueOf(range: Range): Graph[Int,UnDiEdge] =
    Graph.from (
      nodes = range,
      edges = for (i <- range; j <- range; if j != i) yield i~j
    )

  def directedRingOf(range: Range, ascending: Boolean = true): Graph[Int,UnDiEdge] =
    Graph.from (
      nodes = range,
      edges =
        for (i <- range; j = if (i < range.last) i+1 else range.min) yield
          if (ascending) i~>j else j~>i
    )

  def gridOf(width: Int, height: Int, startingAt: Int = 0, withDiagonals : Boolean = false): Graph[Int,UnDiEdge] =
  {
    val edges =
      for {
        x <- 0 until width
        y <- 0 until height
        (dx, dy) <- if (withDiagonals) Seq((1, 0), (0, 1), (1, 1), (-1, 1)) else Seq((1, 0), (0, 1))
        if 0 <= x + dx && x + dx < width && y + dy < height
        i = startingAt + x + y * width
        j = startingAt + (x+dx) + (y+dy) * width
      } yield i ~ j
    val nodes = startingAt until (startingAt + height * width)
    Graph.from(nodes = nodes, edges = edges)
  }

  def torusOf(width: Int, height: Int, startingAt: Int = 0, withDiagonals : Boolean = false): Graph[Int,UnDiEdge] =
  {
    val edges =
      for {
        x <- 0 until width
        y <- 0 until height
        (dx, dy) <- if (withDiagonals) Seq((1, 0), (0, 1), (1, 1), (-1, 1)) else Seq((1, 0), (0, 1))
        if 0 <= x + dx
        tx = (x + dx) % width
        ty = (y + dy) % height
        i = startingAt + x  + y  * width
        j = startingAt + tx + ty * width
      } yield i ~ j
    val nodes = startingAt until (startingAt + height * width)
    Graph.from(nodes = nodes, edges = edges)
  }


  private var seeds = Map.empty[Range, Long]

  def randomGraphOf(range: Range): Graph[Int,UnDiEdge] =
  {
    if (! seeds.isDefinedAt(range)) seeds += ( range -> { scala.util.Random.nextLong() } )
    val seed   = seeds(range)
    val random = new scala.util.Random(seed)

    val N = range.size
    val threshold = if (N>1) math.log(N) / (N-1) else 1
    Graph.from(
      nodes = range,
      edges =
        for {
          i <- range
          j <- range
          if j > i && random.nextDouble() < threshold
        } yield i ~ j
    )
  }

  def geometricRandomGraphOf(range: Range, alpha: Double = 0): Graph[Int,UnDiEdge] =
  {
    case class Position(x:Double, y:Double, rank:Int)

    if (!seeds.isDefinedAt(range)) seeds += (range -> { scala.util.Random.nextLong() })
    val seed   = seeds(range)
    val random = new scala.util.Random(seed)

    val N = range.size
    // compute r near the critical radius for connectivity [see PhD dissertation of Chen Avin]
    val r_square = (math.log(N) + alpha) / (math.Pi * N)
    val r = math.sqrt( r_square )

    val positions =
      for (i <- range;
           x = random.nextDouble();
           y = random.nextDouble())
      yield new Position(x,y,i)

    val edges = for {
      loc1 <- positions
      loc2 <- positions
      if (loc1.rank < loc2.rank) &&
         math.pow (loc1.x - loc2.x, 2) + math.pow (loc1.y - loc2.y, 2) < r_square
    } yield loc1.rank ~ loc2.rank

    Graph.from(nodes = range, edges = edges)
  }
}
