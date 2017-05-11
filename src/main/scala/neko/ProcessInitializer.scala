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

import neko.kernel.{ Dispatcher, NekoSystem }
import neko.trace.EventTracer

@deprecated(
  message = "The class neko.NekoProcessConfig has been deprecated in favor of the class neko.ProcessConfig.",
  since = "0.19.0"
)
class NekoProcessConfig(
    system: NekoSystem,
    pid: PID,
    dispatcher: Dispatcher,
    tracer: EventTracer
) extends ProcessConfig(system, pid, dispatcher, tracer)



class ProcessConfig(
    val system: NekoSystem,
    val pid: PID,
    val dispatcher: Dispatcher,
    val tracer: EventTracer
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
 * Implements the initialization of a process by creating the protocols and connecting them.
 * The initializer is executed once for each process upon their initialization.
 *
 * Example:
 * {{{
 *   class PingPongInitializer extends NekoProcessInitializer
 *   {
 *     override def apply (config: NekoProcessConfig): Unit =
 *     {
 *       val app  = config.register(new PingPong(config))
 *       val fifo = config.register(new FIFOChannel(config))
 *       app --> fifo
 *     }
 *   }
 * }}}
 *
 */
@deprecated(
  message = "The class neko.NekoProcessInitializer has been deprecated in favor of the class neko.ProcessInitializer.",
  since = "0.19.0"
)
trait NekoProcessInitializer extends Function[NekoProcessConfig, Unit]
{
  /**
   * Creates and connects protocols for a given process.
   *
   * Given the configuration of a process, the method creates the protocols for that process,
   * registering them to the configuration through [[neko.NekoProcessConfig.register]] and
   * connecting them through [[neko.ReactiveProtocol.-->]] or [[neko.ActiveProtocol.-->]].
   *
   * Example:
   * {{{
   *   class PingPongInitializer extends NekoProcessInitializer
   *   {
   *     override def apply (config: NekoProcessConfig): Unit =
   *     {
   *       val app  = config.register(new PingPong(config))
   *       val fifo = config.register(new FIFOChannel(config))
   *       app --> fifo
   *     }
   *   }
   * }}}
   *
   * In order to discriminate code based on the process identifier, it is possible to obtain and
   * test the identifier of the process being initialized through [[neko.NekoProcessConfig.pid]].
   *
   * @param config configuration for a given process.
   */
  def apply(config: NekoProcessConfig): Unit
}


@deprecated(
  message = "The class neko.NekoProcessInitializer has been deprecated in favor of the class neko.ProcessInitializer.",
  since = "0.19.0"
)
object NekoProcessInitializer
{
  import scala.util.Try

  /**
   * creates a new instance of [[NekoProcessInitializer]] from the given class name.
   * @param className name of the subclass of [[NekoProcessInitializer]] to instantiate.
   * @return `Success` with the new instance if successful, or `Failure` otherwise.
   */
  def forName(className: String): Try[NekoProcessInitializer] =
    Try {
      Class
        .forName(className)
        .asSubclass(classOf[NekoProcessInitializer])
        .newInstance()
    }
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
 * Without a call to [[ProcessInitializer$.forProcess]], the initializer does nothing by default, thus resulting in
 * an "empty" process.
 */
trait ProcessInitializer extends Function[ProcessConfig, Unit] with NekoProcessInitializer
{
  private var init : Function[ProcessConfig, Unit] = { p => /* nothing */ }
  final def apply(p: ProcessConfig)     { init(p) }
  final def apply(p: NekoProcessConfig) { init(p) }

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
  protected[this] def forProcess (initializer: ProcessConfig => Unit) { init = initializer }

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
        .newInstance()
    }
  
  def apply(initializer: ProcessConfig => Unit): ProcessInitializer =
    new ProcessInitializer
    {
      forProcess(initializer)
    }
}
