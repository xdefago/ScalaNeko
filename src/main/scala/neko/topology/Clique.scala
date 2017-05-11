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

import neko.PID

import scalax.collection.Graph
import scalax.collection.GraphEdge.UnDiEdge

class Clique(range: Range) extends Topology
{
  val processSet : Set[PID] = range.map(PID).toSet
  val size = range.size

  def neighborsFor (process: PID): Option[Set[PID]] =
    if (processSet.contains(process)) Some(processSet - process) else None

  def numberOfEdges: Int = (size * (size-1)) / 2
  def contains (process: PID): Boolean = processSet.contains(process)
  def isConnected: Boolean = true

  lazy val underlying: Graph[Int, UnDiEdge] = Topology.cliqueOf(range)

  def diff (that: Topology): Topology =
    if (this.processSet.intersect(that.processSet).isEmpty) this
    else new Combinator.Diff(this, that)

  def intersect (that: Topology): Topology =
    new Combinator.Intersect(this, that)

  def union (that: Topology): Topology =
    if (that.processSet.subsetOf(this.processSet)) this
    else new Combinator.Union(this, that)
}


/**
 * Factory object to create a clique topologyDescriptor.
 */
object Clique extends TopologyFactory
{
  /**
   * Generates a clique (i.e,. fully connected) topologyDescriptor over the range passed as argument.
   * By default, the clique is formed over all processes.
   *
   * @param range   the range of process numbers on which to generate the clique.
   *           By default, the clique is formed over all processes.
   * @return
   */
  def apply(range: Range) : Topology = new Clique(range)
  def apply(n: Int) : Topology       = this(0 until n)
}
