/**
 *
 * Copyright 2014 Xavier Defago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by IntelliJ IDEA.
 * User: defago
 * Date: 03/07/2014
 * Time: 22:26
 *
 */
package neko

import neko.kernel.{PeriodicTask, Scheduler, SimpleTask, Task}
import neko.util.Time


/**
 * identifier of a task obtained from scheduling an action through an instance of [[Timer]].
 * Tasks identifiers are totally ordered, which allows to compare them.
 *
 * @param value identifying number of the task.
 */
case class TaskID(value: Long) extends ID[Long] with Ordered[TaskID]
{
  type SameType = TaskID

  def name = s"τ[$value]"
  protected def idWith(newID: Long): TaskID = if (value == newID) this else copy(value = newID)

  def compare (that: TaskID): Int = this.value compare that.value
}


object TaskID
{
  private var lastID: Long = 0
  protected[neko] def autoIncrement() : TaskID = {
    lastID += 1
    TaskID(lastID)
  }
}


/**
 * supports the scheduling of delayed and periodic tasks.
 *
 * Some example to illustrate the use of a timer:
 * {{{
 *   val timer = new Timer(...)
 *
 *   val myTask = timer.periodically(Time.second) { t =>
 *      // do something every second
 *      timer.continueWhile(hasSomethingToDo)
 *   }
 *
 *   val myOtherTask = timer.scheduleAfter(Time.second) { t =>
 *      // do something one second later
 *   }
 *
 *   timer.cancel(myOtherTask) // ... or not.
 * }}}
 *
 * @param scheduler   underlying scheduler to manage tasks created by this timer.
 */
class Timer(val scheduler: Scheduler)
{
  /**
   * Returns the current value of the clock elapsed since the beginning of the execution.
   * The value is in simulated time if the system runs as a simulation.
   *
   * @return the current time
   */
  def now: Time = scheduler.currentTime

  /**
   * returns `true` if there are no more scheduled tasks.
   * @return `true` if there are no more scheduled tasks.
   */
  def isEmpty: Boolean  = scheduler.isEmpty

  /**
   * returns `true` if there are still some scheduled tasks.
   * @return `true` if there are still some scheduled tasks.
   */
  def nonEmpty: Boolean = scheduler.nonEmpty

  /**
   * returns `true` if there are no more scheduled tasks, not counting daemon tasks.
   * @return `true` if there are no more scheduled tasks, not counting daemon tasks.
   */
  def isEmptyBesideDaemons: Boolean  = scheduler.isEmptyBesideDaemons

  /**
   * returns `true` if there are still some scheduled tasks, not counting daemon tasks.
   * @return `true` if there are still some scheduled tasks, not counting daemon tasks.
   */
  def nonEmptyBesideDaemons: Boolean = scheduler.nonEmptyBesideDaemons

  /**
   * Creates a new task which will be executed once, after the specified delay.
   * To make the code more readable, one can use [[continueWhile]] or [[continueUntil]] as
   * syntactic sugar.
   *
   * @param delay   delay after which the task will be executed.
   * @param action  the action to execute periodically. It is rescheduled if it returns `true`.
   * @return        identifier of the newly created task.
   */
  def scheduleAfter (delay: Time)(action: Time => Unit): TaskID =
    scheduler.schedule { now =>
      SimpleTask(now+delay, action)
    }.id

  /**
   * Creates a new task which will be executed once, at a specified time (or immediately if already past).
   * To make the code more readable, one can use [[continueWhile]] or [[continueUntil]] as
   * syntactic sugar.
   *
   * @param time    time at which the task will be executed.
   * @param action  the action to execute periodically. It is rescheduled if it returns `true`.
   * @return        identifier of the newly created task.
   */
  def scheduleAt (time: Time)(action: Time => Unit): TaskID =
    schedule(SimpleTask(time, action))

  /**
   * Creates a new periodic task to periodically execute `action`, until it returns `false`.
   * Same as
   * [[periodically(delay:neko\.util\.Time,period:neko\.util\.Time)(action:neko\.util\.Time=>Boolean):neko\.TaskID*]]
   * but the first delay is the same as the period.
   *
   * (Time,Time)(Time⇒Boolean):TaskID*
   *
   * @param period  interval between periodic executions of the task, and delay until the first execution.
   * @param action  the action to execute periodically. It is rescheduled if it returns `true`.
   * @return        identifier of the newly created task.
   */
  def periodically (period: Time)(action: Time => Boolean): TaskID =
    periodically(period, period)(action)

  /**
   * Creates a new periodic task to periodically execute `action`, until it returns `false`.
   * To make the code more readable, one can use [[continueWhile]] or [[continueUntil]] as
   * syntactic sugar.
   *
   * @param delay   delay until the first execution of the task.
   * @param period  interval between periodic executions of the task.
   * @param action  the action to execute periodically. It is rescheduled if it returns `true`.
   * @return        identifier of the newly created task.
   */
  def periodically (delay: Time, period: Time)(action: Time => Boolean): TaskID =
    scheduler.schedule { now =>
      PeriodicTask (scheduler, now + delay, period, action)
    }.id

  /**
   * The task is rescheduled if true. Syntactic sugar to clarify the last condition in periodic task.
   *
   * @param cond  the task is rescheduled if the condition is true
   * @return
   */
  @inline final def continueWhile(cond: => Boolean): Boolean = cond

  /**
   * The task is rescheduled if false. Syntactic sugar to clarify the last condition in periodic task.
   *
   * @param cond  the task is rescheduled if the condition is false
   * @return
   */
  @inline final def continueUntil(cond: => Boolean): Boolean = ! cond

  def schedule(task: Task): TaskID =
  {
    scheduler.schedule(task)
    task.id
  }

  /**
   * Cancels a running task if it is found in the execution queue.
   *
   * @param id  identifier of the task to cancel
   */
  def cancel(id: TaskID) =
    scheduler.cancel(id)
}
