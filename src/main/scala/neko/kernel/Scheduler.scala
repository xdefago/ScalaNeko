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

import com.typesafe.scalalogging.LazyLogging
import neko.util.Time
import neko.{ TaskID, Timer }

import scala.collection.SortedSet


abstract class Scheduler extends LazyLogging
{
  private var taskQueue    = SortedSet.empty[Task]
  private var _currentTime = Time.ZERO

  def newTimer(): Timer = new Timer(scheduler=this)

  def isEmpty  = synchronized { taskQueue.isEmpty  }
  def nonEmpty = synchronized { taskQueue.nonEmpty }

  def isEmptyBesideDaemons  = synchronized { taskQueue.forall(t => t.isDaemon) }
  def nonEmptyBesideDaemons = synchronized { taskQueue.exists(t => ! t.isDaemon) }

  def currentTime = synchronized { _currentTime }

  def schedule(task: Task): Task =
    synchronized {
      logger.trace(s"schedule($task)")
      taskQueue = taskQueue union Set(task)
      task
    }

  def schedule(gen: (Time)=>Task): Task =
    synchronized {
      val task = gen(_currentTime)
      schedule(task)
    }

//  def cancel(task: Task): Unit = cancel(task.id)
  def cancel(taskID: TaskID): Unit =
    synchronized {
      logger.trace(s"cancel(${taskID.name})")
      taskQueue = taskQueue.filterNot(t => t.id == taskID)
      // NB: it is not sufficient to simply remove the task with '-' because, with periodic tasks,
      // the instance currently in the queue could be different from the one passed as parameter,
      // due to periodic reschedule.
    }


  def dumpStatus: String = synchronized {
    s"Scheduler{ ${_currentTime} | " + taskQueue.mkString(", ") + " }"
  }

  private object ExecutorLock

  protected[this] def executeBatch(time: Time, tasks: SortedSet[Task]): Unit

  def executeNext(): Unit =
    ExecutorLock.synchronized {
      val batch: Option[SortedSet[Task]] =
        synchronized {
          if (taskQueue.nonEmpty) {
            _currentTime = _currentTime.succ max taskQueue.head.time

            val (batch, remainder) = taskQueue.partition(_.time <= _currentTime)
            taskQueue = remainder
            Some(batch)
          }
          else None
        }

      logger.trace(s"executeNext(): ${batch.fold("_")(_.map(_.id.name).mkString(", "))}")

      batch foreach {executeBatch(_currentTime, _)}
    }
}


object Scheduler
{
  def apply(): Scheduler = _defaultScheduler
  def defaultScheduler   = _defaultScheduler

  lazy val _defaultScheduler = optDefaultScheduler.getOrElse(newDefaultScheduler())

  def setDefaultScheduler(scheduler: Scheduler): Unit =
  {
    optDefaultScheduler = Some(scheduler)
  }

  private var optDefaultScheduler : Option[Scheduler] = None

  def newDefaultScheduler() : Scheduler = new SequentialScheduler()

}
