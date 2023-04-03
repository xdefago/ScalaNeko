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
package neko.util

import scalax.collection.Graph
import scalax.collection.GraphEdge.{ DiEdge, UnDiEdge }
import scalax.collection.GraphPredef._


trait Topology
{
  def topology : Graph[Int,UnDiEdge]
  def neighborhood : Set[Int]

  def toDotString : String = Topology.toDotString(topology)
}


object Topology
{

  def toDotString(g: Graph[Int,UnDiEdge]) : String =
  {
    val edgeSet  = edgeSetToOuter(g.edges)
    val edgesDot = edgeSet.map {
        case e : DiEdge[Int]   => s"${e.from} -> ${e.to}"
        case e : UnDiEdge[Int] => s"${e._1} -- ${e._2}"
        case _ => ""
      }
    (if (g.isDirected) "digraph G {" else "graph G {") +
      edgesDot.mkString(
        s"\n    overlap = scale;\n    ",
        s";\n    ",
        s";\n}"
      )
  }


  /**
   * Factory object to create a ring topologyDescriptor.
   */
  object Ring
  {
    /**
     * Generates a bidirectional ring topologyDescriptor over the range passed as argument.
     *
     * @param range   the range of process numbers on which to generate the ring.
     *                By default, the ring is formed over all processes.
     */
    def apply(range: Range) : Graph[Int,UnDiEdge] = {
      Graph.from (
        nodes = range,
        edges = for (i <- range; j = if (i < range.last) i+1 else range.min) yield i~j
      )
    }
    def apply(n: Int) : Graph[Int,UnDiEdge] = this.apply(0 until n)
  }


  /**
   * Factory object to create an oriented ring topologyDescriptor.
   */
  object DirectedRing
  {
    /**
     * Generates a unidirectional ring topologyDescriptor over the range passed as argument.
     * By default, the ring is formed over all processes in increasing order.
     *
     * @param range       the range of process numbers on which to generate the ring.
     *                    By default, the ring is formed over all processes.
     * @param decreasing  True if the ring is in decreasing order, and false if increasing (false by default).
     * @return
     */
    def apply(range: Range, decreasing : Boolean = false) : Graph[Int,DiEdge] =
    {
      Graph.from (
        nodes = range,
        edges =
          for (i <- range; j = if (i < range.last) i+1 else range.min) yield
            if (decreasing) j~>i else i~>j
      )
    }

    def apply(n: Int, decreasing : Boolean) : Graph[Int,DiEdge] = this.apply(0 until n, decreasing)
    def apply(n: Int) : Graph[Int,DiEdge]                       = this.apply(0 until n)
  }


  /**
   * Factory object to create a clique topologyDescriptor.
   */
  object Clique
  {
    /**
     * Generates a clique (i.e,. fully connected) topologyDescriptor over the range passed as argument.
     * By default, the clique is formed over all processes.
     *
     * @param range   the range of process numbers on which to generate the clique.
          *           By default, the clique is formed over all processes.
     * @return
     */
    def apply(range: Range) : Graph[Int,UnDiEdge] =
      Graph.from (
        nodes = range,
        edges = for (i <- range; j <- range; if j != i) yield i~j
      )

    def apply(n: Int) : Graph[Int,UnDiEdge] = this.apply(0 until n)
  }


  /**
   * Factory object to create a random graph (of type Erdös-Renyi).
   *
   * The threshold for deciding whether an edge exists is log(n) / (n-1), where n is the
   * size of the range given as parameter.
   * This choice is because this is a little above the bond percolation threshold for a
   * clique.
   */
  object Random
  {
    private var seeds = Map.empty[Range, Long]

    /**
     * Generates a random graph using Erdös-Renyi's method and using log(n) / (n-1) as the
     * probability of existence of an edge over a clique.
     *
     * @param range the range of process numbers on which to generate the random graph.
     * @return
     */
    def apply(range: Range) : Graph[Int,UnDiEdge] =
    {
      if (! seeds.isDefinedAt(range)) seeds += ( range -> { util.Random.nextLong() } )
      val seed   = seeds(range)
      val random = new util.Random(seed)

      val N = range.size
      val threshold = if (N>1) math.log(N) / (N-1) else 1
      Graph.from(
        nodes = range,
        edges = for {
          i <- range
          j <- range
          if j > i && random.nextDouble() < threshold
        } yield i ~ j
      )
    }

    def apply(n: Int) : Graph[Int,UnDiEdge] = this.apply(0 until n)
  }


  /**
   * Factory object to create a geometric random graph.
   *
   * The idea is to generate positions for the nodes uniformly at random in a 1 x 1 square plane.
   * Then, given a radius r, two nodes are neighbors iff. their Euclidean distance is less than r.
   *
   * The critical radius is calculated from the following formula (linked to percolation theory):
   *
   * {{{
   *   val r = math.sqrt( (math.log(N) + alpha) / (math.Pi * N))
   * }}}
   * where alpha is an adjustment parameter to increase the likelihood that the resulting graph is
   * connected.
   */
  object GeometricRandom
  {
    private var seeds = Map.empty[Range, Long]

    /**
     * Generates a geometric random graph, in which nodes are randomly positioned on the plane, and
     * an edge exists between two nodes only if their distance is less than some chosen constant.
     *
     * @param range   the range of process numbers on which to generate the random graph.
     * @param alpha   parameter to increase the likelihood that the resulting graph is connected.
     * @return
     */
    def apply(range: Range, alpha : Double = 0) : Graph[Int,UnDiEdge] =
    {
      case class Position(x:Double, y:Double, rank:Int)

      if (!seeds.isDefinedAt(range)) seeds += (range -> { util.Random.nextLong() })
      val seed = seeds(range)
      val random = new util.Random(seed)

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

    def apply(n: Int, alpha: Double) : Graph[Int,UnDiEdge] = this.apply(0 until n, alpha)
    def apply(n: Int) : Graph[Int,UnDiEdge]                = this.apply(0 until n)
  }


  /**
   * Factory object to generate a grid topologyDescriptor.
   */
  object Grid
  {
    /**
     * Generates a grid topologyDescriptor, with process 0 at one corner.
     *
     * The grid generated is a possibly incomplete square.
     *
     * @param range the range of process numbers on which to generate the grid
     * @return
     */
    def apply(range: Range) : Graph[Int,UnDiEdge] =
    {
      val N    = range.size
      val side = math.ceil(math.sqrt(N)).toInt

      val edges = for {
        i <- range
        x = i % side
        y = i / side
        (dx, dy) <- Seq ((-1, 0), (0, -1))
        if x + dx >= 0 && y + dy >= 0
      } yield i ~ (i + dx + side*dy)
      Graph.from(nodes = range, edges = edges)
    }

    def apply(n: Int) : Graph[Int,UnDiEdge] = this.apply(0 until n)
  }
}
