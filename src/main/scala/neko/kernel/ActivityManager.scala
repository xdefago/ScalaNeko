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

import java.util.concurrent.CyclicBarrier

import com.typesafe.scalalogging.LazyLogging
import neko.ID
import neko.exceptions.{ ActivityAbortedError, InitializationError }

import scala.concurrent.OnCompleteRunnable

/**
 * identifier of an activity. Activities are represented by the trait [[ManagedActivity]].
 * @param value unique number identifying the activity
 */
protected[neko] case class ActivityID(value: Int) extends ID[Int]
{
  type SameType = ActivityID
  def name: String = s"Act($value)"
  protected def idWith (newID: Int): SameType = ActivityID(newID)
}


/**
 * handles the lockstep execution of registered concurrent activities
 * (protocol objects that implement the trait [[ManagedActivity]]).
 *
 * The activities are assumed to exchange information using a producer-consumer model such as
 * [[java.util.concurrent.BlockingQueue]]. The model is not safe if the activity is actually be
 * made to wait on the queue (see details at the bottom).
 *
 * Initialization:
 *  1. new activities are registered by calling [[registerActivity]], which provides an identifier
 *     ([[ActivityID]]).
 *  1. after all activities have been registered, the activity manager is started [[start]].
 *  1. the activities are started just like normal threads.
 *
 * The lifecycle of an activity is as follows:
 *  1. upon starting, the activity first calls [[willStart]] once, and starts its concurrent
 *     execution as a thread.
 *  1. Each time the activity would be required to wait on a queue (checked by polling on the
 *     structure), it will instead wait through a call to [[willWait]], and proceed to do the
 *     blocking call only once it is sure that it will not be made to wait.
 *  1. Once the activity has finished its normal execution, it calls [[willFinish]] which blocks
 *     until all other threads have finished.
 *
 * After all activities started, the lockstep execution proceeds as follows:
 *  1. all activities execute concurrently until all of them are blocked on a call to [[willWait]]
 *     or [[willFinish]].
 *  1. the manager executes all registered actions (i.e., actions registered through
 *     [[registerAction]]) sequentially, and in mutual exclusion with the activities.
 *  1. after all actions have been executed once,
 *     the cycle repeats if some activity has called [[willWait]], or
 *     the cycle ends if all activities have called [[willFinish]].
 *
 * ===Additional notes on synchronization===
 *
 * As said before, activities can't be made to actually wait on a queue. This means that the queue
 * must provide a means for polling (such as [[java.util.concurrent.BlockingQueue!.isEmpty]]), to
 * make sure that the thread will not actually be blocked by the queue. Practically speaking, this
 * means that:
 *  - some scala synchronization primitives like [[scala.concurrent.SyncVar]] or
 *    [[scala.concurrent.SyncChannel]] can't be used safely.
 *  - the queue can have only one consumer.
 *  - the queue must either be unbounded (e.g., [[java.util.concurrent.LinkedBlockingQueue]] as
 *    used in the implementation of [[neko.ActiveProtocol]]) or it must have a reserved location
 *    for every producer activity.
 */
protected[neko] class ActivityManager(val system: NekoSystem) extends LazyLogging
{
  import ActivityManager._

  private class ActivityInfo(val id: ActivityID, val activity: ManagedActivity, var status: Status)

  private var registeredActivities = IndexedSeq.empty[ActivityInfo]
  private var started     = false
  private var allFinished = false
  private var barrier : CyclicBarrier = _
  private var barrierActions = List.empty[()=>Unit]

  /**
   * returns true if all activities have finished.
   *
   * An activity is considered having finished if it has called the method [[willFinish]].
   *
   * @return whether all activities have finished.
   */
  def allActivitiesFinished: Boolean = allFinished

  def abnormallyTerminated: Option[Set[String]] =
  {
    val res =
      registeredActivities
        .collect { case act: ActivityInfo if act.status == Status.Aborted => act.activity.name }
        .toSet
    if (res.nonEmpty) Some(res) else None
  }

  def registerActivity(activeProtocol: ManagedActivity): ActivityID =
    synchronized {
      logger.trace(s"registerActivity(${activeProtocol.name})")
      if (started) {
        if (! logger.underlying.isErrorEnabled)
          throw new InitializationError("Can't add activities dynamically after the manager has started")
        else
          logger.error("Tried to register an activity after ActivityManager has already started. (IGNORED)")
      }
      val id   = ActivityID(registeredActivities.size)
      val info = new ActivityInfo(id, activeProtocol, Status.Created)
      registeredActivities = registeredActivities :+ info
      logger.trace(s"Activity registry: $id -> ${info.activity.name}")
      id
    }

  def registerAction(action: ()=>Unit): Unit =
    synchronized {
      logger.trace(s"registerAction(...) -> ${barrierActions.size+1} actions registered")
      if (! started) {
        barrierActions = action :: barrierActions
      } else {
        if (! logger.underlying.isErrorEnabled)
          throw new InitializationError("Tried to register an action after ActivityManager has already started. (UNSUPPORTED)")
        else
          logger.error("Tried to register an action after ActivityManager has already started. (IGNORED)")
      }
    }

  /**
   * an activity indicates that it will be waiting for a condition to be satisfied (i.e., it is
   * polling on the condition).
   *
   * The call is putting the thread to sleep for a while, and the condition must be checked again
   * after that.
   * @param id identifier of the activity
   */
  def willWait(id: ActivityID) =
  {
    logger.trace(s"willWait(${registeredActivities(id.value).activity.name})")
    registeredActivities(id.value).status = Status.Waiting
    barrier.await()
    if (registeredActivities(id.value).status == Status.Aborted) {
      throw new ActivityAbortedError(s"Abnormal termination for activity ${id.name}")
    }
    registeredActivities(id.value).status = Status.Running
  }

  /**
   * an activity indicates that it has finished its execution and will be waiting until all
   * activities have done so.
   *
   * The call is blocking until all other activities have also finished.
   *
   * @param id identifier of the activity
   * @return
   */
  def willFinish(id: ActivityID) =
  {
    registeredActivities(id.value).status = Status.Finished

    logger.trace(
      s"willFinish(${
        registeredActivities(id.value)
          .activity
          .name
      }). Unfinished = $unfinishedActivities"
    )

    do {
      barrier.await()
    } while (! allActivitiesFinished)
  }

  /**
   * an activity indicates that it is ready to start its execution.
   *
   * The call is non-blocking.
   *
   * @param id identifier of the activity
   */
  def willStart(id: ActivityID): Unit =
  {
    logger.trace(s"willStart(${registeredActivities(id.value).activity.name})")
    registeredActivities(id.value).status = Status.Running
  }

  def reset(): Unit =
  {
    // assert (! hasPendingMessages)
    logger.warn(s"reset(): changing status of activities: Waiting -> Finished")
    registeredActivities.foreach { act =>
      if (act.status != Status.Finished) {
        logger.debug(s"${act.id.name}: ${act.status} -> Aborted (forced)")
        act.status = Status.Aborted
      }
    }

    AllFinished.synchronized {
      allFinished = true
      AllFinished.notifyAll()
    }

    //barrier.reset()
  }

  /**
   * returns true if some activity is waiting and has pending messages.
   * @return
   */
  def hasPendingMessages: Boolean =
    registeredActivities.exists(a => a.status == Status.Waiting && a.activity.hasPendingMessages)

  /**
   * returns a string that lists all registered activities that haven't yet finished. This is
   * intended for debugging and logging purpose.
   *
   * @return a string listing all registered activities that haven't yet finished
   */
  def unfinishedActivities: String =
  {
    registeredActivities
      .filterNot(_.status == Status.Finished)
      .map(acti => s"${acti.activity.name}/${acti.status}")
      .mkString(", ")
  }

  private def registeredActivitiesAsString: String =
  {
    def highlight (s: String): String = s"##$s##"
    registeredActivities.map { acti =>
      val name = acti.activity.name
      if (acti.status != Status.Created) highlight(name) else name
    }.mkString(", ")
  }

  private def dumpActivitiesStatus: String =
  {
    registeredActivities.map { acti =>
      val name   = acti.activity.name
      val status = acti.status
      s"$name/$status"
    }.mkString(", ")
  }

  /**
   * starts the execution of the activity manager.
   *
   * After calling this method, it is no longer possible to register new activities
   * (i.e., [[registerActivity]]), nor to add
   * any new actions (i.e., [[registerAction]]), and any such attempt will either result in an
   * exception or an error log message.
   */
  def start(whenDone: => Unit): Unit =
    synchronized {
      logger.trace(s"start()")
      if (! started) {
        logger.trace(s"REGISTERED: $registeredActivitiesAsString")
        assume(registeredActivities.forall(_.status == Status.Created))
        started = true

        if (registeredActivities.nonEmpty) {
          registeredActivities.foreach { info => info.status = Status.Running }

          // register cyclic Runnable executing the cyclic actions
          val action = new Runnable with OnCompleteRunnable {
            val actions = barrierActions.reverse
            def run() = {
              logger.trace(s"Managed Activities: MUTEX START")
              logger.trace(s"Scheduler actions : $dumpActivitiesStatus")

              // check if all finished
              if (registeredActivities.forall(_.status == Status.Finished)) {
                logger.trace("All finished")
                allFinished = true
              }

              // execute all exclusive actions
              actions.foreach { act => act() }

              // notify potentially joining processes
              if (allFinished) {
                logger.debug("Finished executing all pending actions: cleanup")
                whenDone

                logger.debug("Finished executing all pending actions: notify main thread")
                AllFinished.synchronized {
                  AllFinished.notifyAll()
                }
              }
              logger.trace(s"Managed Activities: MUTEX LEAVE")
            }
          }
          barrier = new CyclicBarrier(registeredActivities.size, action)
        } else {
          logger.debug("Finished executing all pending actions: cleanup")
          whenDone

          logger.debug("Finished executing all pending actions: notify main thread")
          AllFinished.synchronized {
            allFinished = true
          }
        }
      }
    }

  /**
   * waits until all activities have finished.
   *
   * This method allows an external thread to synchronize on the manager and wait until the
   * lockstep execution has finished. Beware that this method cannot be called by any of the
   * registered activities, as this would result in a deadlock.
   */
  def join(): Unit = AllFinished.synchronized { while (! allFinished) AllFinished.wait() }

  private object AllFinished
}

protected[neko] object ActivityManager
{
  sealed trait Status
  object Status
  {
    case object Created  extends Status
    case object Running  extends Status
    case object Waiting  extends Status
    case object Finished extends Status
    case object Aborted  extends Status
  }
}
