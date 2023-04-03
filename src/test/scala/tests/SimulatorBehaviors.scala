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

import neko.TaskID
import neko.kernel.sim.Simulator
import neko.util.Time
import org.scalatest.flatspec.AnyFlatSpec

trait SimulatorBehaviors extends TimerBehaviors
{ this: AnyFlatSpec =>

  def discreteEventSimulator(newSimulator: => Simulator): Unit = {
    it should behave like normalTimer(newSimulator)

    it should "process tasks until empty" in {
      val sim = newSimulator

      assert(sim.scheduler.isEmpty)

      val completed  = Array.fill[Option[Time]](numIterations)(None)
      val tasks =
        for (i <- completed.indices)
          yield sim.scheduleAfter(Time(i+1)){ t => completed(i) = Some(t) }

      assert(sim.scheduler.nonEmpty)

      sim.run()

      assert(sim.scheduler.isEmpty)
      assertResult(Time(numIterations))(sim.now)
      for (i <- completed.indices) {
        assertResult(Some(Time(i+1)))(completed(i))
      }
    }

    it should "cancel periodic tasks properly" in {
      val sim = newSimulator

      var iteration = 0
      val completed = Array.fill[Option[Time]](numIterations)(None)
      val task : TaskID = sim.periodically(Time(100)){ t =>
        completed(iteration) = Some(t)
        iteration += 1
        iteration < numIterations // <- condition to reschedule
      }

      assert(sim.scheduler.nonEmpty)

      sim.run()

      assert(sim.scheduler.isEmpty)
      assertResult(numIterations)(iteration)
      for (i <- completed.indices) {
        assertResult(Some(Time(100 + 100 * i)))(completed(i))
      }
    }

    it should "execute periodic tasks until finished" in {
      val sim = newSimulator

      val iterations = Array.fill[Int](numIterations)(0)
      val completed  = Array.fill[Option[Time]](numIterations)(None)
      val tasks: IndexedSeq[TaskID] =
        for (i <- iterations.indices)
          yield sim.periodically(Time(3*i+1), Time(7*i+1)){
            t =>
              completed(i)  = Some(t)
              iterations(i) = iterations(i) + 1
              sim.continueWhile(iterations(i) < numIterations)
          }

      assert(sim.scheduler.nonEmpty)

      sim.run()

      assert(sim.scheduler.isEmpty)
      for (i <- iterations.indices) {
        assertResult(numIterations)(iterations(i))
      }
    }
  }
}
