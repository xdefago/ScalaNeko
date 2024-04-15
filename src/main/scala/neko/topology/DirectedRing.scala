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


class DirectedRing(range: Range, ascending: Boolean = true) extends Topology
{
  val processSet : Set[PID] = range.map(i => PID(i)).toSet
  val size = range.size

  private def nextOf(p: PID): PID = p.map { i => ((i+1-range.min) % size) + range.min }
  private def prevOf(p: PID): PID = p.map { i => ((i+size-1-range.min) % size) + range.min }

  def neighborsFor (process: PID): Option[Set[PID]] =
    if (processSet.contains(process)) {
      if (ascending)
        Some(Set(nextOf(process)))
      else
        Some(Set(prevOf(process)))
    } else None

  def numberOfEdges: Int = size
  def contains (process: PID): Boolean = processSet.contains(process)
  def isConnected: Boolean = true

  def underlying: Graph[Int, UnDiEdge] = Topology.directedRingOf(range, ascending)

  def diff (that: Topology): Topology =
    if (this.processSet.intersect(that.processSet).isEmpty) this else new Combinator.Diff(this, that)

  def intersect (that: Topology): Topology = new Combinator.Intersect(this, that)
  def union (that: Topology): Topology     = new Combinator.Union(this, that)
}

/**
 * Factory object to create an oriented ring topologyDescriptor.
 */
object DirectedRing extends TopologyFactory
{
  /**
   * Generates a unidirectional ring topologyDescriptor over the range passed as argument.
   * By default, the ring is formed over all processes in increasing order.
   *
   * @param range       the range of process numbers on which to generate the ring.
   *                    By default, the ring is formed over all processes.
   * @param ascending  True if the ring is in increasing order, and false if decreasing.
   * @return
   */
  def apply(range: Range, ascending : Boolean) : Topology =
    new DirectedRing(range, ascending)

  def apply(range: Range) : Topology                = this.apply(range, ascending = true)
  def apply(n: Int, ascending : Boolean) : Topology = this.apply(0 until n, ascending)
  def apply(n: Int) : Topology                      = this.apply(0 until n)
}
