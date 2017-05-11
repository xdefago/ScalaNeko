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

import neko.kernel.{ PeriodicTask, Scheduler, SimpleTask }
import neko.util.Time
import org.scalatest.FlatSpec

trait SchedulerBehaviors
{ this: FlatSpec =>

  def normalScheduler(newScheduler: => Scheduler): Unit = {
    val numIterations = 10

    it should "start at time zero and be empty initially" in {
      val sched = newScheduler
      assertResult(Time(0))(sched.currentTime)
      assertResult(true)(sched.isEmpty)
      assertResult(false)(sched.nonEmpty)
    }

    it should "not advance time when empty" in {
      val sched = newScheduler
      assertResult(Time(0))(sched.currentTime)
      for (i <- 1 to numIterations) {
        sched.executeNext()
        assertResult(Time(0))(sched.currentTime)
      }
    }

    it should "advance time properly when executing tasks" in {
      val sched = newScheduler
      assertResult(Time(0))(sched.currentTime)
      for (i <- 1 to numIterations) {
        val time = Time(i * 123)
        val task = SimpleTask(time, { (t) => })
        sched.schedule(task)
        sched.executeNext()
        assertResult(time)(sched.currentTime)
      }
    }

    it should "advance time by epsilons when executing older tasks" in {
      val sched = newScheduler
      assertResult(Time(0))(sched.currentTime)
      for (i <- 1 to numIterations) {
        val task = SimpleTask(Time(0), { (t) => })
        sched.schedule(task)
        sched.executeNext()
        assertResult(Time(i))(sched.currentTime)
      }
    }

    it should "execute tasks with different times one at a time when scheduled in batch" in {
      val sched = newScheduler
      assertResult(Time(0))(sched.currentTime)
      val tasks = for (i <- 1 to numIterations) yield (Time(i), SimpleTask(Time(i), {t=> }))

      for ((_,task) <- tasks.reverse) {
        sched.schedule(task)
      }

      assertResult(Time(0))(sched.currentTime)

      for ((time, _) <- tasks) {
        sched.executeNext()
        assertResult(time)(sched.currentTime)
      }
    }

    it should "execute simple tasks with different times in sequence" in {
      val sched = newScheduler
      val completed = Array.fill[Option[Time]](numIterations)(None)

      assertResult(Time(0))(sched.currentTime)

      val tasks =
        for (i <- 1 to numIterations)
          yield (Time(i), SimpleTask(Time(i), {t=> completed(i-1) = Some(t) }))

      for ((_,task) <- tasks.reverse) {
        sched.schedule(task)
      }

      assertResult(Time(0))(sched.currentTime)
      assert(completed.forall(_.isEmpty))

      for (((time, _), i) <- tasks.zipWithIndex) {
        sched.executeNext()
        assertResult(time)(sched.currentTime)
        assertResult(Some(time))(completed(i))
      }
    }

    it should "execute simple tasks with same time in batch" in {
      val sched = newScheduler
      val completed  = Array.fill[Option[Time]](numIterations)(None)
      val targetTime = Time(123)
      assertResult(Time(0))(sched.currentTime)

      val tasks =
        for (i <- 1 to numIterations)
          yield (targetTime, SimpleTask(targetTime, { t=> completed(i-1) = Some(t) }))

      for ((_,task) <- tasks.reverse) {
        sched.schedule(task)
      }

      assertResult(Time(0))(sched.currentTime)
      assert(completed.forall(_.isEmpty))

      sched.executeNext()
      assertResult(targetTime)(sched.currentTime)
      assert(sched.isEmpty)
      for (((time, _), i) <- tasks.zipWithIndex) {
        assertResult(Some(time))(completed(i))
      }
    }

    it should "execute periodic tasks repeatedly and cancel them properly" in {
      val sched = newScheduler
      val completed  = Array.fill[Option[Time]](numIterations)(None)
      val start  = Time(123)
      val period = Time(54)
      var index = 0
      val task  = PeriodicTask(sched, start, period, { t=> completed(index) = Some(t) ; index += 1; true })
      val expectedTimes = for (i <- 0 until numIterations) yield start + period * i
      assertResult(Time(0))(sched.currentTime)

      sched.schedule(task)
      for (expected <- expectedTimes) {
        sched.executeNext()
        assert(sched.nonEmpty)
        assertResult(expected)(sched.currentTime)
      }

      sched.cancel(task.id)
      assert(sched.isEmpty)
    }

    it should "cancel simple tasks properly" in {
      val sched = newScheduler
      val rangeDiscard = 2 to 6

      val tasks =
        for (i <- 1 to numIterations)
          yield (Time(i*100), SimpleTask(Time(i*100), { t=> }))
      assert(sched.isEmpty)

      for ((_, task) <- tasks) {
        sched.schedule(task)
      }
      assert(sched.nonEmpty)

      for (i <- rangeDiscard) {
        val (_, task) = tasks(i)
        sched.cancel(task.id)
      }
      assert(sched.nonEmpty)

      for {
        i <- tasks.indices if !rangeDiscard.contains(i)
        (time, _) = tasks(i)
      }{
        sched.executeNext()
        assertResult(time)(sched.currentTime)
      }
      assert(sched.isEmpty)
    }
  }
}
