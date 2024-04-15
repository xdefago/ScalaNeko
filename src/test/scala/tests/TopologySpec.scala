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
package tests

import neko.PID
import neko.topology._
import org.scalatest.flatspec.AnyFlatSpec

class TopologySpec extends AnyFlatSpec
{
  val ranges = List(0 until 10, 5 until 15, 2 until 30, 10 until 100, 0 until 1000)

  behavior of "topology.Clique"

  it should "have correct creation and behave like a clique" in {
    for (range <- ranges) {
      val clique = Clique(range)

      assertResult(range.size)(clique.processSet.size)
      assertResult(PID(range.min))(clique.processSet.min)
      assertResult(PID(range.max))(clique.processSet.max)

      assert(clique.isConnected)

      assertResult(range.size)(clique.size)
      assertResult(range.size * (range.size-1) / 2)(clique.numberOfEdges)

      clique.processSet.foreach { p =>
        val neighborsOpt = clique.neighborsFor(p)
        assert(neighborsOpt.isDefined)

        neighborsOpt.foreach { neighbors =>
          assertResult (range.size - 1)(neighbors.size)
          assert (!neighbors.contains (p))
          assertResult(clique.processSet-p)(neighbors)
        }
      }
    }
  }

  behavior of "topology.Ring"

  it should "have correct creation and behave like a ring" in {
    for (range <- ranges) {
      val ring = Ring(range)

      assertResult(range.size)(ring.processSet.size)
      assertResult(PID(range.min))(ring.processSet.min)
      assertResult(PID(range.max))(ring.processSet.max)

      assert(ring.isConnected)

      assertResult(range.size)(ring.size)
      assertResult(range.size)(ring.numberOfEdges)

      ring.processSet.foreach { p =>
        val next = p.map { i => range.min + (i-range.min+1) % range.size }
        val prev = p.map { i => range.min + (i-range.min+range.size-1) % range.size }

        val neighborsOpt = ring.neighborsFor(p)
        assert(neighborsOpt.isDefined)

        neighborsOpt.foreach { neighbors =>
          assertResult (2)(neighbors.size)
          assert (! neighbors.contains (p))
          assertResult(Set(prev, next))(neighbors)
        }
      }
    }
  }

  behavior of "topology.DirectedRing"

  it should "have correct creation and behave like a directed ring" in {
    for (range <- ranges) {
      val ring = DirectedRing(range)

      assertResult(range.size)(ring.processSet.size)
      assertResult(PID(range.min))(ring.processSet.min)
      assertResult(PID(range.max))(ring.processSet.max)

      assert(ring.isConnected)

      assertResult(range.size)(ring.size)
      assertResult(range.size)(ring.numberOfEdges)

      ring.processSet.foreach { p =>
        val next = p.map { i => range.min + (i-range.min+1) % range.size }
        val prev = p.map { i => range.min + (i-range.min+range.size-1) % range.size }

        val neighborsOpt = ring.neighborsFor(p)
        assert(neighborsOpt.isDefined)

        neighborsOpt.foreach { neighbors =>
          assertResult (1)(neighbors.size)
          assert (! neighbors.contains (p))
          assertResult(Set(next))(neighbors)
        }
      }
    }
  }

  behavior of "topology.Grid"

  it should "have correct creation and behave like a grid" in {
    for (range <- ranges) {
      val grid = Grid (range)
      val side = math.ceil (math.sqrt (range.size)).toInt

      assertResult (side * side)(grid.processSet.size)
      assertResult (PID (range.min))(grid.processSet.min)
      assertResult (PID (range.min + side * side - 1))(grid.processSet.max)

      assert (grid.isConnected)

      assertResult (side * side)(grid.size)
      assertResult (2 * (side - 1) * side)(grid.numberOfEdges)

      grid.processSet.foreach { p =>
        val neighborsOpt = grid.neighborsFor (p)
        assert (neighborsOpt.isDefined)

        neighborsOpt.foreach { neighbors =>
          assert (neighbors.size >= 2 || side < 2)
          assert (neighbors.size <= 4)
          assert (!neighbors.contains (p))
        }
      }
    }
  }

  behavior of "topology.Random"

  it should "have correct creation and behave like a random graph" in {
    for (range <- ranges) {
      val randomGraph = Random (range)

      assertResult (range.size)(randomGraph.processSet.size)
      assertResult (PID (range.min))(randomGraph.processSet.min)
      assertResult (PID (range.max))(randomGraph.processSet.max)

      assert (randomGraph.isConnected)

      assertResult (range.size)(randomGraph.size)
      assert (randomGraph.numberOfEdges >= range.size)
      assert (randomGraph.numberOfEdges < range.size * range.size)

      randomGraph.processSet.foreach { p =>

        val neighborsOpt = randomGraph.neighborsFor (p)
        assert (neighborsOpt.isDefined)

        neighborsOpt.foreach { neighbors =>
          assert (neighbors.nonEmpty)
          assert (!neighbors.contains (p))
        }
      }
    }
  }

  behavior of "topology.GeometricRandom"

  it should "have correct creation and behave like a random graph" in {
    for (range <- ranges) {
      val randomGraph = GeometricRandom (range)

      assertResult (range.size)(randomGraph.processSet.size)
      assertResult (PID (range.min))(randomGraph.processSet.min)
      assertResult (PID (range.max))(randomGraph.processSet.max)

      assert (randomGraph.isConnected)

      assertResult (range.size)(randomGraph.size)
      assert (randomGraph.numberOfEdges >= range.size)
      assert (randomGraph.numberOfEdges < range.size * range.size)

      randomGraph.processSet.foreach { p =>

        val neighborsOpt = randomGraph.neighborsFor (p)
        assert (neighborsOpt.isDefined)

        neighborsOpt.foreach { neighbors =>
          assert (neighbors.nonEmpty)
          assert (!neighbors.contains (p))
        }
      }
    }
  }

  behavior of "Combinator.Union"

  it should "connect two consecutive rings" in {
    val range1 = 0 to 10
    val range2 = 10 to 20

    assertResult(Set(10))((range1 intersect range2).toSet)

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 union ring2

    assert(combi.isConnected)
    assertResult(range1.size + range2.size - 1)(combi.size)
    assertResult(ring1.numberOfEdges + ring2.numberOfEdges)(combi.numberOfEdges)

    combi.processSet.foreach { p =>
      if (p != PID(10)) {
        assertResult (2)(combi.neighborsFor(p).fold(0)(_.size))
      }
      else {
        assertResult (4)(combi.neighborsFor(p).fold(0)(_.size))
      }
    }
  }

  it should "connect two overlapping rings" in {
    val range1 = 0 to 10
    val range2 = 5 to 15
    val intersectRange = (range1 intersect range2).toSet

    assertResult((5 to 10).toSet)(intersectRange)

    val intersect = intersectRange.map(i => PID(i))

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 union ring2

    assert(combi.isConnected)
    assertResult(range1.size + range2.size - intersect.size)(combi.size)
    assertResult(ring1.numberOfEdges + ring2.numberOfEdges - intersect.size + 1)(combi.numberOfEdges)

    combi.processSet.foreach { p =>
      val neighbors      = combi.neighborsFor(p).getOrElse(Set.empty)
      val neighborsCount = neighbors.size

      if (p == intersect.min || p == intersect.max) {
        assertResult (3, p->neighbors)(neighborsCount)
      }
      else {
        assertResult (2, p->neighbors)(neighborsCount)
      }
    }
  }

  it should "not connect independent rings" in {
    val range1 = 0 to 10
    val range2 = 15 to 25
    val intersectRange = (range1 intersect range2).toSet

    assertResult(Set.empty)(intersectRange)

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 union ring2

    assert( ! combi.isConnected)
    assertResult(range1.size + range2.size)(combi.size)
    assertResult(ring1.numberOfEdges + ring2.numberOfEdges)(combi.numberOfEdges)

    combi.processSet.foreach { p =>
      val neighbors      = combi.neighborsFor(p).getOrElse(Set.empty)
      val neighborsCount = neighbors.size

      assertResult (2, p->neighbors)(neighborsCount)
    }
  }

  it should "combine cliques" in {
    val range1 = 0 to 10
    val range2 = 10 to 20

    assertResult(Set(10))((range1 intersect range2).toSet)

    val clique1 = Clique(range1)
    val clique2 = Clique(range2)
    val combi = clique1 union clique2

    assert(combi.isConnected)
    assertResult(range1.size + range2.size - 1)(combi.size)
    assertResult(clique1.numberOfEdges + clique2.numberOfEdges)(combi.numberOfEdges)


    combi.processSet.foreach { p =>
      val neighbors      = combi.neighborsFor(p).getOrElse(Set.empty)
      val neighborsCount = neighbors.size

      if (p == PID(10)) {
        assertResult (clique1.size + clique2.size - 2)(neighborsCount)
      }
      else if (clique1.processSet.contains(p)) {
        assertResult (clique1.size - 1, p->neighbors)(neighborsCount)
      }
      else if (clique2.processSet.contains(p)) {
        assertResult (clique2.size - 1, p->neighbors)(neighborsCount)
      }
      else {
        assert(false)
      }
    }
  }

  behavior of "Combinator.Intersect"

  it should "result in singleton with two consecutive rings" in {
    val range1 = 0 to 10
    val range2 = 10 to 20

    assertResult(Set(10))((range1 intersect range2).toSet)

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 intersect ring2

    assert(combi.isConnected)
    assertResult(1)(combi.size)
    assertResult(0, combi.processSet)(combi.numberOfEdges)

    assertResult(Set(PID(10)))(combi.processSet)
  }

  it should "result in a path with two overlapping rings" in {
    val range1 = 0 to 10
    val range2 = 5 to 15
    val intersectRange = (range1 intersect range2).toSet

    assertResult((5 to 10).toSet)(intersectRange)

    val intersect = intersectRange.map(i => PID(i))

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 intersect ring2

    assert(combi.isConnected)
    assertResult(intersect.size)(combi.size)
    assertResult(intersect.size - 1)(combi.numberOfEdges)

    combi.processSet.foreach { p =>
      val neighbors      = combi.neighborsFor(p).getOrElse(Set.empty)
      val neighborsCount = neighbors.size

      if (p == intersect.min || p == intersect.max) {
        assertResult (1, p->neighbors)(neighborsCount)
      }
      else {
        assertResult (2, p->neighbors)(neighborsCount)
      }
    }
  }

  it should "be empty with two independent rings" in {
    val range1 = 0 to 10
    val range2 = 15 to 25
    val intersectRange = (range1 intersect range2).toSet

    assertResult(Set.empty)(intersectRange)

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 intersect ring2

    assertResult(0)(combi.size)
    assertResult(0)(combi.numberOfEdges)
    assertResult(Set.empty[PID])(combi.processSet)
  }

  behavior of "Combinator.Diff"

  it should "open a ring from another consecutive rings" in {
    val range1 = 0 to 10
    val range2 = 10 to 20
    val intersectRange = (range1 intersect range2).toSet

    assertResult(Set(10))(intersectRange)

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 diff ring2

    assert(combi.isConnected)
    assertResult(range1.size - intersectRange.size)(combi.size)
    assertResult(ring1.numberOfEdges - intersectRange.size - 1)(combi.numberOfEdges)

    combi.processSet.foreach { p =>
      val neighbors = combi.neighborsFor(p).getOrElse(Set.empty)

      if (p == combi.processSet.min || p == combi.processSet.max) {
        assertResult (1, p->neighbors)(neighbors.size)
      }
      else {
        assertResult (2, p->neighbors)(neighbors.size)
      }
    }
  }

  it should "open a ring from overlapping rings" in {
    val range1 = 0 to 10
    val range2 = 5 to 15
    val intersectRange = (range1 intersect range2).toSet

    assertResult((5 to 10).toSet)(intersectRange)

    val intersect = intersectRange.map(i => PID(i))

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 diff ring2

    assert(combi.isConnected)
    assertResult(range1.size - intersect.size)(combi.size)
    assertResult(ring1.numberOfEdges - intersect.size - 1)(combi.numberOfEdges)

    combi.processSet.foreach { p =>
      val neighbors      = combi.neighborsFor(p).getOrElse(Set.empty)
      val neighborsCount = neighbors.size

      if (p == combi.processSet.min || p == combi.processSet.max) {
        assertResult (1, p->neighbors)(neighborsCount)
      }
      else {
        assertResult (2, p->neighbors)(neighborsCount)
      }
    }
  }

  it should "not change when diff'd from independent rings" in {
    val range1 = 0 to 10
    val range2 = 15 to 25
    val intersectRange = (range1 intersect range2).toSet

    assertResult(Set.empty)(intersectRange)

    val ring1 = Ring(range1)
    val ring2 = Ring(range2)
    val combi = ring1 diff ring2

    assert(combi.isConnected)
    assertResult(ring1.size)(combi.size)
    assertResult(ring1.numberOfEdges)(combi.numberOfEdges)

    combi.processSet.foreach { p =>
      val neighbors      = combi.neighborsFor(p).getOrElse(Set.empty)
      val neighborsCount = neighbors.size

      assertResult (2, p->neighbors)(neighborsCount)
    }
  }

}
