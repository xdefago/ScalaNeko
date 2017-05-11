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

import ch.qos.logback.classic.{ Level, Logger => LogbackLogger }
import neko.config.NekoConfig
import neko.kernel.sim.NekoSimSystem
import neko.kernel.{ Initializer, NekoSystem }
import neko.topology._
import org.slf4j.{ Logger, LoggerFactory }


/**
 * Basic class used to define the system.
 *
 * An object that extends this class will act as a replacement for the main object, and also as a
 * replacement for the configuration file used in original Neko.
 * The parameters are used to create the system. This must provide the total number of processes,
 * as well as the initializer class for the processes. The class provided must necessarily be a
 * subclass of [[neko.NekoProcessInitializer]].
 *
 * For instance, the code below declares a system consisting of three processes, each of which is
 * initialized by a class <tt>HelloWorldInitializer</tt>:
 * {{{
 * object Main extends NekoMain(N=3, initializer=classOf[HelloWorldInitializer])
 * }}}
 *
 * @param N                 the total number of processes
 * @param initializer  the class for the initializer of processes
 * @param logLevel          optionally sets the log level (default is OFF)
 * @param logFile           optionally provides a filename on which to write logs (not yet supported)
 */
@deprecated(
  message = "The class neko.NekoMain has been deprecated in favor of the class neko.Main.",
  since="0.18.0"
)
class NekoMain (
    N: Int,
    initializer: Class[_ <: NekoProcessInitializer],
    logLevel: Level = Level.ERROR,
    logFile: Option[String] = None,
    withTrace: Boolean = false,
    traceFile: String = "default",
    topology: TopologyFactory = Clique
)
  extends App
{
  // TODO: Change so that one gives the topology itself, and that N is calculated based on it
  // TODO: Output a report of settings (incl. topology) to the console at the beginning of the execution
  // TODO: Output a report with statistics at the end of the execution
  // TODO: rationalize the io of traces
  //
  // longer term:
  // TODO: see about providing an optional GUI to io the console of processes, network, system
  // TODO: reintegrate support for actual distributed execution (rely on Akka?)
  
  private final val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]
  root.setLevel(logLevel)

  final val logger: Logger = LoggerFactory.getLogger(classOf[NekoMain])

  if (withTrace) {
    neko.trace.Tracing.defaultTracer_=(neko.trace.Tracer.fileOnly(traceFile, topology(N)))
  } else {
    neko.trace.Tracing.defaultTracer_=(neko.trace.Tracer.consoleOnly)
  }
  
  
  private val config     = Initializer.configFor(N, initializer, logLevel, logFile)
  private val nekoConfig = NekoConfig(config, topology, neko.trace.Tracing.defaultTracer)

//  if (withTrace) {
//    neko.trace.Tracing.defaultTracer_=(neko.trace.Tracer.consoleOnly)
//  }

  logger.info("Starting")

  val system: NekoSystem = new NekoSimSystem(nekoConfig)

  system.mainloop(
    onFinish = {t =>
      logger.info(s"Simulation ended normally at time ${t.asSeconds} (${t.asNanoseconds}})")
    })

  logger.info("Exiting")
}



