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

import neko.Timer
import neko.util.Time
import org.scalatest.FlatSpec

trait TimerBehaviors
{ this: FlatSpec =>
  val numIterations = 10

  def normalTimer(newTimer: => Timer): Unit = {
    it should "not advance time when empty" in {
      val timer = newTimer
      val sched = timer.scheduler

      assertResult(Time(0))(timer.now)
      for (i <- 1 to numIterations) {
        sched.executeNext()
        assertResult(Time(0))(timer.now)
      }
    }

    it should "advance time properly when executing tasks" in {
      val timer = newTimer
      val sched = timer.scheduler
      val delay = Time(123)

      assertResult(Time(0))(timer.now)
      for (i <- 1 to numIterations) {
        val time = Time(i * 123)
        timer.scheduleAfter(delay) { (t) => }
        sched.executeNext()
        assertResult(time)(timer.now)
      }
    }

    it should "advance time by epsilons when executing older tasks" in {
      val timer = newTimer
      val sched = timer.scheduler

      assertResult(Time(0))(timer.now)
      for (i <- 1 to numIterations) {
        timer.scheduleAfter(Time(-1)) { (t) => }
        sched.executeNext()
        assertResult(Time(i))(timer.now)
      }
    }

    it should "execute tasks with different times one at a time when scheduled in batch" in {
      val timer = newTimer
      val sched = timer.scheduler

      assertResult(Time(0))(timer.now)
      val tasks =
        for {
          i <- numIterations to 1 by -1
          time = Time(i)
          task = timer.scheduleAt(time) { t=> }
        } yield (time, task)

      assertResult(Time(0))(timer.now)

      for ((time, _) <- tasks.reverse) {
        sched.executeNext()
        assertResult(time)(timer.now)
      }
    }

    it should "execute simple tasks with different times in sequence" in {
      val timer = newTimer
      val sched = timer.scheduler
      val completed = Array.fill[Option[Time]](numIterations)(None)

      assertResult(Time(0))(timer.now)

      val tasks =
        for {
          i <- numIterations to 1 by -1
          time = Time(i)
          task = timer.scheduleAt(time) { t=> completed(i-1) = Some(t) }
        } yield (time, task)

      assertResult(Time(0))(timer.now)
      assert(completed.forall(_.isEmpty))

      for (((time, _), i) <- tasks.reverse.zipWithIndex) {
        sched.executeNext()
        assertResult(time)(timer.now)
        assertResult(Some(time))(completed(i))
      }
    }

    it should "execute simple tasks with same time in batch" in {
      val timer = newTimer
      val sched = timer.scheduler
      val completed  = Array.fill[Option[Time]](numIterations)(None)
      val targetTime = Time(123)

      assertResult(Time(0))(timer.now)

      val tasks =
        for {
          i <- 1 to numIterations
          task = timer.scheduleAt(targetTime) { t=> completed(i-1) = Some(t) }
        } yield (targetTime, task)

      assertResult(Time(0))(timer.now)
      assert(completed.forall(_.isEmpty))

      sched.executeNext()
      assertResult(targetTime)(timer.now)
      assert(sched.isEmpty)
      for (((time, _), i) <- tasks.zipWithIndex) {
        assertResult(Some(time))(completed(i))
      }
    }

    it should "execute periodic tasks repeatedly and cancel them properly" in {
      val timer = newTimer
      val sched = timer.scheduler
      val completed  = Array.fill[Option[Time]](numIterations)(None)
      val start  = Time(123)
      val period = Time(54)
      var index = 0
      val expectedTimes = for (i <- 0 until numIterations) yield start + period * i
      assertResult(Time(0))(sched.currentTime)

      val task = timer.periodically(start, period) { t=> completed(index) = Some(t) ; index += 1 ; true }

      for (expected <- expectedTimes) {
        sched.executeNext()
        assert(sched.nonEmpty)
        assertResult(expected)(timer.now)
      }

      timer.cancel(task)
      assert(sched.isEmpty)
    }

  }
}
