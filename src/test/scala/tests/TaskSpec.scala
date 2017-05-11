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

import neko.kernel.{ PeriodicTask, Scheduler }
import neko.util.Time
import org.scalatest.FlatSpec

class TaskSpec extends FlatSpec
{
  behavior of "PeriodicTask"

  it should "keep the same id through copy" in {
    val sched = Scheduler.newDefaultScheduler()
    val task = PeriodicTask(sched, Time(10), Time(100), { t => true })
    val expectedID = task.id
    val taskCopy = task.copy(time = task.time + task.period)

    assertResult(expectedID)(taskCopy.id)
  }
}
