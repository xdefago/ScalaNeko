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

package neko.kernel

import neko.TaskID
import neko.util.Time


object Task
{
  trait Daemon {
    this: Task =>
      override def isDaemon = true
  }
}


sealed abstract class Task
  extends Ordered[Task]
{
  //val id = TaskID.autoIncrement()
  def id: TaskID

  def time: Time
  def action: (Time) => Unit

  def compare (that: Task): Int =
    (this.time, this.id) compare (that.time, that.id)
    // NB: it is necessary to compare according to hashcodes or else different tasks scheduled
    // at the same would be considered equivalent in priority queues.

  def executeAt(t: Time): Unit = action(t)
  def isDaemon: Boolean = false
}


case class SimpleTask(
  time: Time,
  action: (Time) => Unit,
  id: TaskID = TaskID.autoIncrement())
    extends Task
{
  override def toString: String = s"SimpleTask(${id.name}, $time)"
}


case class PeriodicTask(
  scheduler: Scheduler,
  time: Time, period: Time,
  repeatAction: (Time) => Boolean,
  id: TaskID = TaskID.autoIncrement())
    extends Task
{
  val action: Time => Unit = {
    time: Time =>
      val willReschedule = repeatAction(time)
      if (willReschedule) {
        val nextTask = this.copy(time = time + period, id = this.id)
        scheduler.schedule(nextTask)
      }
  }
  override def toString: String = s"PeriodicTask(${id.name}, $time, $period)"
}
