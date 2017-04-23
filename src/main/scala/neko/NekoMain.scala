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
 * Date: 28/05/2014
 * Time: 14:50
 *
 */
package neko

import ch.qos.logback.classic.{ Level, Logger => LogbackLogger }
import neko.config.NekoConfig
import neko.kernel.sim.NekoSimSystem
import neko.kernel.{ Initializer, NekoSystem }
import neko.topology._
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.mutable.ListBuffer
import scala.compat.Platform.currentTime


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
    TraceFile: String = "default",
    topology: TopologyFactory = Clique
)
  extends App
{
  // TODO: Change so that one gives the topology itself, and that N is calculated based on it
  // TODO: Output a report of settings (incl. topology) to the console at the beginning of the execution
  // TODO: Output a report with statistics at the end of the execution
  // TODO: rationalize the output of traces
  //
  // longer term:
  // TODO: see about providing an optional GUI to output the console of processes, network, system
  // TODO: reintegrate support for actual distributed execution (rely on Akka?)
  
  private final val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]
  root.setLevel(logLevel)

  final val logger: Logger = LoggerFactory.getLogger(classOf[NekoMain])

  if (withTrace) {
    neko.trace.Tracing.defaultTracer_=(neko.trace.Tracer.fileOnly(TraceFile, N, topology))
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


/**
 * Basic class used to define the system.
 *
 * An object that extends this class will act as a replacement for the main object, and also as a
 * replacement for the configuration file used in original Neko.
 * The parameters are used to create the system. This must provide the network topology (for which
 * the number of processes is inferred),
 * as well an initializer for the processes (see [[neko.ProcessInitializer]]).
 *
 * For instance, the code below declares a system consisting of three processes, each of which is
 * initialized by the process initializer provided:
 * {{{
 * object MyMain extends Main(topology.Clique(3))(ProcessInitializer { p=> ... })
 * }}}
 *
 * @param topology     network topology (see [[neko.topology]])
 * @param initializer  the initializer of processes
 * @param logLevel     optionally sets the log level (default is OFF)
 * @param logFile      optionally provides a filename on which to write logs (not yet supported)
 * @param withTrace    controls the generation of a trace of network events (send and receive)
 */
class Main (
    val topology : Topology,
    logLevel : Level = Level.ERROR,
    logFile  : Option[String] = None,
    withTrace : Boolean = false
)(initializer: ProcessInitializer)
//  extends DelayedInit
{
  // TODO: Output a report of settings (incl. topology) to the console at the beginning of the execution
  // TODO: Output a report with statistics at the end of the execution
  // TODO: rationalize the output of traces
  //
  // longer term:
  // TODO: see about providing an optional GUI to output the console of processes, network, system
  // TODO: reintegrate support for actual distributed execution (rely on Akka?)

  def N = topology.size
  
  val topoFactory = TopologyFactory(topology)
  
  val logger: Logger = LoggerFactory.getLogger(classOf[Main])
  
  /** The command line arguments passed to the application's `main` method.
   */
  @deprecatedOverriding("args should not be overridden", "2.11.0")
  protected def args: Array[String] = _args
  
  private var _args: Array[String] = _

  /** The init hook. This saves all initialization code for execution within `main`.
   *  This method is normally never called directly from user code.
   *  Instead it is called as compiler-generated code for those classes and objects
   *  (but not traits) that inherit from the `DelayedInit` trait and that do not
   *  themselves define a `delayedInit` method.
   *  @param body the initialization code to be stored for later execution
   */
//  @deprecated("the delayedInit mechanism will disappear", "2.11.0")
//  override def delayedInit(body: => Unit) { initCode += (() => body) }

  private val initCode = new ListBuffer[() => Unit]
  
  /** The main method.
   * This stores all arguments so that they can be retrieved with `args`
   * and then executes all initialization code segments in the order in which
   * they were passed to `delayedInit`.
   *
   * @param args the arguments passed to the main method
   */
  @deprecatedOverriding("main should not be overridden", "0.18.0")
  def main (args: Array[String]) =
  {
    this._args = args
    
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]
    root.setLevel(logLevel)
    
    val config     = Initializer.configFor(N, classOf[ProcessInitializer], logLevel, logFile)
    val nekoConfig = NekoConfig(config, initializer, topology, neko.trace.Tracing.defaultTracer)
    
    if (withTrace) {
      neko.trace.Tracing.defaultTracer_=(neko.trace.Tracer.consoleOnly)
    }
    
    logger.info("Starting")

    for (proc <- initCode) proc()
    
    val system: NekoSystem = new NekoSimSystem(nekoConfig)

    system.mainloop(
      onFinish = { t =>
        logger.info(s"Simulation ended normally at time ${t.asSeconds } (${t.asNanoseconds }})")
      }
    )
    
    logger.info("Exiting")
  }
}
