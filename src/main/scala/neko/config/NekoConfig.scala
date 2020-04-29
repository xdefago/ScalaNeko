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
package neko.config

import com.typesafe.config.Config
import neko.ProcessInitializer
import neko.network.Network
import neko.topology.{ Topology, TopologyFactory }
import neko.trace.{ EventTracer, NullEventTracer }

import scala.util.{ Success, Try }


class NekoConfig(
    config: Config,
    init: Try[ProcessInitializer],
    topologyDescriptor: TopologyFactory,
    val tracer: EventTracer
)
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
    val initializer     = init
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
  @deprecated(
    message = "The use of NekoProcessInitializer has been deprecated. Use other constructors instead.",
    since = "0.19.0"
  )
  def apply(
      config: Config,
      topologyDescriptor: TopologyFactory,
      tracer: EventTracer): NekoConfig =
    new NekoConfig(
      config,
      ProcessInitializer.forName(config.getString(CF.PROCESS_INIT)),
      topologyDescriptor,
      tracer
    )

  def apply(config: Config, topologyDescriptor: TopologyFactory): NekoConfig =
    this(config, topologyDescriptor, NullEventTracer)
  
  def apply(
      config: Config,
      init: ProcessInitializer,
      topo: Topology,
      tracer: EventTracer = NullEventTracer): NekoConfig =
    new NekoConfig(config, Success(init), TopologyFactory(topo), tracer )
  
  def apply(config: Config, init: ProcessInitializer, topo: Topology): NekoConfig =
    this(config, init, topo, NullEventTracer)
  
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
    def initializer: Try[ProcessInitializer]
  }
}
