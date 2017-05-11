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

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import neko._
import neko.config.NekoConfig
import neko.kernel.sim.Simulator
import neko.network.Network
import neko.util.Time

import scala.concurrent.ExecutionContext
import scala.util.Try


// TODO: do not use inheritance to specialize behavior;
// instead, make a final system class which takes factory functions/objects to provide simulation
// vs execution (or simulation within a different simulation engine)

abstract class NekoSystem(val config: NekoConfig) extends LazyLogging
{
  val executorService = java.util.concurrent.Executors.newCachedThreadPool()
  val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  
  val processNum : Int = config.sys.N

  protected[neko] val activityManager = new ActivityManager(this)

  /**
   * reference to the underlying scheduler for the system.
   *
   * Although it can be used to create new timers, the method [[newTimer]] is the preferred
   * approach to creating timers.
   */
  val scheduler : Scheduler = new SequentialScheduler() // new ParallelScheduler(executionContext)

  // FIXME: this should not be here, but this circumvents a problem with the order of initialization sequence
  val simulator : Simulator = new Simulator(scheduler)

  val timer : Timer         = simulator

  assume(processNum > 0)

  val processIDs: Seq[PID] = for (i <- 0 until processNum) yield PID(i)
  val processSet: Set[PID] = processIDs.toSet

  // INIT.1: create network
  logger.info("INIT: creating networks")
  val network: Network = createNetwork()

  // INIT.2: create processes
  logger.info("INIT: creating processes")
  val processes: Map[PID, NekoProcess] =
    processIDs
      .map { pid => pid -> new NekoProcess(pid, this)(config) }.toMap

  // INIT.3: register processes to networks (currently supports only one network)
  logger.info("INIT: registering processes to networks")
  network.receivers = processes.mapValues { p=> p.receivers.head }

  // INIT.4: start network
  logger.info("INIT: starting networks")
  startNetwork()

  // INIT.5: prestart processes
  logger.info("INIT: prestarting all processes")
  processes.values.foreach { _.preStart() }

  /*
  // INIT.6: create and register sentinel activity
  object SentinelActivity extends Runnable with Protocol with ManagedActivity
  {
    def run (): Unit = {
      while (simulator.nonEmptyBesideDaemons) {
        willWait()
      }
      willFinish()
    }
    protected[neko] def hasPendingMessages: Boolean = false
    def process: PID = PID(-1)
    def system: NekoSystem = NekoSystem.this
    def id: ProtoID = ProtoID("__SENTINEL__")
  }
*/

  // INIT.6: prepare simulator
  logger.info("INIT: readying simulator")
  activityManager.registerAction({
    () =>
      if (activityManager.allActivitiesFinished) {
        if (simulator.isEmptyBesideDaemons) {
          // NORMAL END
          logger.info(s"Simulation ended normally (at ${simulator.now.asSeconds})")
        } else {
          // NORMAL END: some pending tasks
          logger.info(s"Simulation ended (at ${simulator.now.asSeconds}) with pending events: ${simulator.scheduler.dumpStatus}")
          // - record finish time
          // - process all pending events and log them
        }
      } else if (simulator.isEmpty && ! activityManager.hasPendingMessages) {
        // ABNORMAL END: some threads must be waiting
        logger.error(s"Simulation ended (at ${simulator.now.asSeconds}}) with no more events but some waiting threads: ${activityManager.unfinishedActivities}")
        activityManager.reset()
        try {
          executorService.awaitTermination (5, TimeUnit.MINUTES)
        } catch {
          case e : InterruptedException => /* Ignore */
        } finally {
          executorService.shutdownNow ()
        }
      } else {
        // Normal case: advance
        logger.trace(s"advance time (${simulator.now})")
        simulator.advanceTime()
      }
  })
  activityManager.start (whenDone={
    logger.trace("processing pending events (non daemon tasks)")
    while (simulator.nonEmptyBesideDaemons) {
      simulator.advanceTime()
    }
  })

  /**
   * runs the main loop of the execution. The method is blocking until the simulation/execution has
   * completed.
   */
  def mainloop(onFinish: (Time)=>Unit = { t => /* default: do nothing */ }) =
  {
    // INIT.7: start processes
    logger.info("INIT: starting processes")
    processes.values.foreach {_.start()}

    activityManager.join()
    logger.info("JOIN: All activities finished")

    val abnormalSet = activityManager.abnormallyTerminated

    activityManager.abnormallyTerminated.fold {

      /* FINISH NORMALLY */
      val endTime = timer.now
      processes.values.foreach {_.shutdown()}
      onFinish(endTime)
      executorService.shutdown()

    } { actSet =>

      /* ABNORMAL TERMINATION */
      logger.error("ABNORMAL TERMINATION: aborted processes -> " + actSet.mkString(", "))
      executorService.shutdownNow()
    }
  }

  // ---- ABSTRACT
  protected[this] def createNetwork(): Network
  protected[this] def startNetwork(): Unit =
  {
    network.preStart()
    network.start()
  }

  /**
   * returns the current time (simulation time if the system is simulated).
   *
   * @return the current time
   */
  def currentTime: Time = simulator.now

  /**
   * returns a new timer for scheduling events on the current system.
   *
   * @return a new timer instance
   */
  def newTimer(): Timer = timer
}



object NekoSystem
{
  /**
   * creates a new instance of [[NekoSystem]] from the given class name.
   * @param className name of the subclass of [[NekoSystem]] to instantiate.
   * @return `Success` with the new instance if successful, or `Failure` otherwise.
   */
  def forName(className: String)(config: NekoConfig): Try[NekoSystem] =
    Try {
      Class
        .forName(className)
        .asSubclass(classOf[NekoSystem])
        .getConstructor(classOf[NekoConfig])
        .newInstance(config)
    }

  /**
   * creates a new instance of [[NekoSystem]] from the given class.
   * @param clazz subclass of the [[NekoSystem]] to instantiate.
   * @return `Success` with the new instance if successful, or `Failure` otherwise.
   */
  def forClass[T<:NekoSystem](clazz: Class[T])(config: NekoConfig): Try[T] =
    Try {
      clazz
        .getConstructor(classOf[NekoConfig])
        .newInstance(config)
    }
}
