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
package neko

import java.io.PrintWriter

import neko.kernel.{ Dispatcher, NekoSystem }
import neko.trace.EventTracer


class ProcessConfig(
    val system: NekoSystem,
    val pid: PID,
    val dispatcher: Dispatcher,
    val tracer: EventTracer,
    val out: PrintWriter
) {
  private var _registeredProtocols = Set.empty[Protocol]

  def register[T<:Protocol](proto: T): T =
  {
    _registeredProtocols = _registeredProtocols + proto
    proto
  }

  def registeredProtocols = _registeredProtocols
}



/**
 * Basic trait for implementing the process initialization.
 *
 * A process needs an initializer class to instantiate its protocols, register them, and connect
 * them together.
 * Such a class must be a subclass of [[ProcessInitializer]] and contain the code for
 * initialization as shown in the example below.
 *
 * {{{
 * class LamportMutexInitializer extends ProcessInitializer
 * {
 *   forProcess { p =>
 *     // create protocols
 *     val app   = new MutexApplication(p)
 *     val clock = new protocol.LamportClock(p)
 *     val mutex = new LamportMutex(p, clock)
 *     val fifo  = new protocol.FIFOChannel(p)
 *
 *     // connect protocols
 *     app   --> mutex
 *     mutex --> clock
 *     clock --> fifo
 *   }
 * }
 * }}}
 *
 * Below is an alternative to create an initializer that is more adapted when
 * used as argument for the [[neko.Main]] class.
 * {{{
 * ProcessInitializer { p =>
 *   // create protocols
 *   val app   = new MutexApplication(p)
 *   val clock = new protocol.LamportClock(p)
 *   val mutex = new LamportMutex(p, clock)
 *   val fifo  = new protocol.FIFOChannel(p)
 *
 *   // connect protocols
 *   app   --> mutex
 *   mutex --> clock
 *   clock --> fifo
 * }
 * }}}
 *
 *
 * Without a block, the initializer does nothing by default, thus resulting in
 * an "empty" process.
 */
trait ProcessInitializer extends Function[ProcessConfig, Unit]
{
  private var init : Function[ProcessConfig, Unit] = { p => /* nothing */ }
  final def apply(p: ProcessConfig) = { init(p) }

  /**
   * Initialization code for a process.
   *
   * The argument is an initializer function which, given a process configuration as argument,
   * creates the protocols for that process, registers them, and connects them. For instance,
   * {{{
   * class LamportMutexInitializer extends ProcessInitializer
   * {
   *   forProcess { p =>
   *     // create protocols
   *     val app   = new MutexApplication(p)
   *     val clock = new protocol.LamportClock(p)
   *     val mutex = new LamportMutex(p, clock)
   *     val fifo  = new protocol.FIFOChannel(p)
   *
   *     // connect protocols
   *     app   --> mutex
   *     mutex --> clock
   *     clock --> fifo
   *   }
   * }
   * }}}
   *
   * @param initializer  the block which, given a process configuration as argument, creates and
   * connects the protocols.
   */
  protected[this] def forProcess (initializer: ProcessConfig => Unit): Unit = { init = initializer }

  /**
   * Initialization code for a process. (same as [[ProcessInitializer.forProcess]])
   *
   * @param initializer the block which, given a process configuration as argument, creates and
   *                    connects the protocols
   */
  protected[this] def init(initializer: Function[ProcessConfig,Unit]) = forProcess(initializer)
}


object ProcessInitializer
{
  import scala.util.Try

  /**
   * creates a new instance of [[ProcessInitializer]] from the given class name.
   * @param className name of the subclass of [[ProcessInitializer]] to instantiate.
   * @return `Success` with the new instance if successful, or `Failure` otherwise.
   */
  def forName(className: String): Try[ProcessInitializer] =
    Try {
      Class
        .forName(className)
        .asSubclass(classOf[ProcessInitializer])
        .getConstructor()
        .newInstance()
    }
  
  def apply(initializer: ProcessConfig => Unit): ProcessInitializer =
    new ProcessInitializer
    {
      forProcess(initializer)
    }
}
