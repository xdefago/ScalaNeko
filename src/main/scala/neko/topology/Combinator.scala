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

object Combinator
{
  class Union(t1: Topology, t2: Topology) extends Topology
  {
    lazy val processSet : Set[PID] = t1.processSet union t2.processSet
    lazy val size = processSet.size

    def neighborsFor (process: PID): Option[Set[PID]] =
      if (processSet.contains(process)) {
        val n1 = t1.neighborsFor (process).getOrElse (Set.empty)
        val n2 = t2.neighborsFor (process).getOrElse (Set.empty)
        val res = n1 union n2
        if (res.isEmpty) None else Some (res)
      } else None

    def numberOfEdges: Int = underlying.graphSize
    def contains (process: PID): Boolean = processSet.contains(process)
    lazy val isConnected: Boolean = underlying.isConnected

    lazy val underlying: Graph[Int, UnDiEdge] = t1.underlying union t2.underlying

    def diff (that: Topology): Topology      = new Diff(this, that)
    def intersect (that: Topology): Topology = new Intersect(this, that)
    def union (that: Topology): Topology     = new Union(this, that)
  }

  class Intersect(t1: Topology, t2: Topology) extends Topology
    {
      lazy val processSet : Set[PID] = t1.processSet intersect  t2.processSet
      lazy val size = processSet.size

      def neighborsFor (process: PID): Option[Set[PID]] =
      {
        val n1 = t1.neighborsFor(process).getOrElse(Set.empty)
        val n2 = t2.neighborsFor(process).getOrElse(Set.empty)
        val res = n1 intersect n2
        if (res.isEmpty) None else Some(res)
      }

      lazy val numberOfEdges: Int = underlying.graphSize

      def contains (process: PID): Boolean = t1.contains(process) && t2.contains(process)
      lazy val isConnected: Boolean = underlying.isConnected

      lazy val underlying: Graph[Int, UnDiEdge] = t1.underlying intersect t2.underlying

    def diff (that: Topology): Topology      = new Diff(this, that)
    def intersect (that: Topology): Topology = new Intersect(this, that)
    def union (that: Topology): Topology     = new Union(this, that)
    }

  class Diff(t1: Topology, t2: Topology) extends Topology
      {
        lazy val processSet : Set[PID] = t1.processSet diff  t2.processSet
        lazy val size = processSet.size

        def neighborsFor (process: PID): Option[Set[PID]] =
        {
          val n1 = t1.neighborsFor(process).getOrElse(Set.empty)
          val res = n1 diff t2.processSet
          if (res.isEmpty) None else Some(res)
        }

        lazy val numberOfEdges: Int = underlying.graphSize

        def contains (process: PID): Boolean = t1.contains(process) && ! t2.contains(process)
        lazy val isConnected: Boolean = underlying.isConnected

        lazy val underlying: Graph[Int, UnDiEdge] = t1.underlying diff t2.underlying

        def diff (that: Topology): Topology      = new Diff(this, that)
        def intersect (that: Topology): Topology = new Intersect(this, that)
        def union (that: Topology): Topology     = new Union(this, that)
      }
}
