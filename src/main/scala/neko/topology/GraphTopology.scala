/**
 *
 * Copyright 2015 Xavier Defago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by IntelliJ IDEA.
 * User: defago
 * Date: 27/06/15
 * Time: 21:29
 *
 */
package neko.topology

import neko._

import scalax.collection.Graph
import scalax.collection.GraphEdge.UnDiEdge


trait GraphTopology extends Topology
{
  val underlying : Graph[Int,UnDiEdge]

  lazy val processSet: Set[PID] =
    for (node <- underlying.nodes.toOuter) yield node match { case i:Int => PID(i) }

  override def neighborsFor(process: PID): Option[Set[PID]] =
  {
    val optNode = underlying.find(process.value)
    val p = underlying.get(process.value)
    optNode
      .map { p =>
      val neighborhood: Set[Int] = p.neighbors.map {_.toInt}
      neighborhood.map(PID)
    }
  }

  override def contains(process: PID): Boolean =
    (underlying find process.value).isDefined

  override def isConnected: Boolean = underlying.isConnected
  override def size: Int            = underlying.order
  override def numberOfEdges: Int   = underlying.graphSize

  def union    (that: Topology): Topology = combine(that)( (g1,g2) => g1 union g2 )
  def diff     (that: Topology): Topology = combine(that)( (g1,g2) => g1 diff g2 )
  def intersect(that: Topology): Topology = combine(that)( (g1,g2) => g1 intersect g2 )

  def combine(that: Topology)(f:(Graph[Int,UnDiEdge],Graph[Int,UnDiEdge])=>Graph[Int,UnDiEdge]): Topology =
  {
    val newUnderlying = f(this.underlying, that.underlying)
    new GraphTopology
    {
      val underlying = newUnderlying
    }
  }
}
