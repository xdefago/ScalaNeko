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
 * Date: 30/05/15
 * Time: 12:55
 *
 */
package neko.config

import com.typesafe.config.Config
import neko.NekoProcessInitializer
import neko.network.Network
import neko.topology.TopologyFactory

import scala.util.Try


class NekoConfig(config: Config, topologyDescriptor: TopologyFactory)
{
  import scala.collection.JavaConverters._

  val isSimulation = config.getBoolean(CF.SIMULATION)

  val network = new NekoConfig.NetworkConf {
    val className = config.getString(CF.NETWORK)
    val clazz     = Network.forName(className)
    val lambda    = config.getDouble(CF.NETWORK_LAMBDA)
    val topology  = topologyDescriptor
  }

  val process = new NekoConfig.ProcessConf {
    val initializerName = config.getString(CF.PROCESS_INIT)
    val initializer     = NekoProcessInitializer.forName(initializerName)
  }

  val sys = new NekoConfig.SystemConf {
    val N = config.getInt(CF.PROCESS_NUM)
  }

  object log {
    val handlers: List[String] = config.getStringList(CF.LOG_HANDLERS).asScala.toList
    val level    = config.getString(CF.LOG_LEVEL)
  }
}

object NekoConfig
{
  def apply(config: Config, topologyDescriptor: TopologyFactory): NekoConfig =
    new NekoConfig(config, topologyDescriptor)

  trait SystemConf
  {
    def N: Int
  }

  trait NetworkConf
  {
    def className: String
    def clazz: Try[Class[_<:Network]]
    def lambda: Double
    def topology: TopologyFactory
  }

  trait ProcessConf
  {
    def initializer: Try[NekoProcessInitializer]
  }
}
