/**
 *
 * Copyright 2015 Xavier Defago
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
 * Date: 10/06/15
 * Time: 13:00
 *
 */
package neko.kernel

import java.io.File
import java.net.{MalformedURLException, URL}

import ch.qos.logback.classic.Level
import com.typesafe.config.{Config, ConfigFactory}
import neko.config.CF

object Initializer
{
  def configFor(
    processNum : Int,
    initializer: Class[_],
    logLevel: Level,
    logFile:  Option[String] = None): Config =
  {
    val console = "java.util.logging.ConsoleHandler"
    val configMap = Map(
      "neko.simulation" -> true,
      CF.PROCESS_NUM  -> processNum,
      CF.PROCESS_INIT -> initializer.getName,
      //
      // ---- The network used for communication.
      CF.NETWORK -> "neko.network.sim.RandomSimNetwork",
      CF.NETWORK_LAMBDA -> 10.0,
      //
      // ---- Logging options
      CF.LOG_HANDLERS -> console,
      CF.LOG_LEVEL    -> logLevel.levelStr
    )
    fromMap(configMap)
  }

  val baseConfig = ConfigFactory.load()

  def fromArgs(args: Array[String]): Config = {
    if (args.length != 1) {
      System.err.println("Usage: program_name configuration_file")
      System.exit(2)
    }

    try {
      fromURL(new URL(args(0)))
    } catch {
      case _ : MalformedURLException =>
        fromFile(new File(args(0)))
    }
  }

  def fromMap(configMap: Map[String, _<:Any]): Config = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseMap(configMap.asJava)
  }

  def fromFile(file: File): Config = { ConfigFactory.parseFile(file) }
  def fromURL(url: URL): Config    = { ConfigFactory.parseURL(url) }
}
