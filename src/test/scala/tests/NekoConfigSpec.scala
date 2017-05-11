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
package tests

import com.typesafe.config.{ Config, ConfigFactory }
import neko.config._
import org.scalatest.FlatSpec
import tests.init.DummyProcessInitializer

import scala.util.Random


class NekoConfigSpec extends FlatSpec
{
  val randSeed = 0xcafebabefeedbeefL
  val rand = new Random(randSeed)
  val sampSimulation = Set(true, false)
  val sampNum = Set(0, rand.nextInt(Int.MaxValue), Int.MaxValue)
  val sampProcInit = Set(classOf[DummyProcessInitializer].getName)
  val sampNet = Set(classOf[neko.network.sim.RandomSimNetwork].getName)
  val sampLambda = Set(0.0, rand.nextGaussian(), Double.MaxValue)
  val sampLogHandlers = Set(
    List("java.util.logging.ConsoleHandler"),
    List.empty[String],
    List("java.util.logging.ConsoleHandler", "java.util.logging.FileHandler")
  )
  val sampLogLevel = Set("debug", "warning", "error")

  val samples =
    for {
      sim <- sampSimulation
      procNum <- sampNum
      procInit <- sampProcInit
      net <- sampNet
      lambda <- sampLambda
      logHandlers <- sampLogHandlers
      logLevel <- sampLogLevel
    } yield (sim, procNum, procInit, net, lambda, logHandlers, logLevel)

  def configFor(
    simulation: Boolean,
    processNum: Int,
    procInit: String,
    net: String,
    lambda: Double,
    logHandlers: List[String],
    logLevel: String
  ): Config =
    ConfigFactory.parseString(
    s"""
      |neko {
      |  simulation = $simulation
      |  process {
      |    num = $processNum
      |    initializer = $procInit
      |  }
      |  network {
      |    class = $net
      |    lambda = $lambda
      |  }
      |  log {
      |    handlers = ${logHandlers.mkString("[ ", ", ", " ]")}
      |    level = $logLevel
      |  }
      |}
    """.stripMargin
    )

  behavior of "NekoConfig"

  it should "load basic information from Config" in {
    for ((sim, procNum, procInit, net, lambda, logHandlers, logLevel) <- samples) {
      val config = configFor(sim, procNum, procInit, net, lambda, logHandlers, logLevel)
      val nekoConf = NekoConfig(config, neko.topology.Clique)
      assertResult(sim)(nekoConf.isSimulation)
      assertResult(procNum)(nekoConf.sys.N)
      assert(nekoConf.process.initializer.isSuccess)
      assertResult(net)(nekoConf.network.className)
      assertResult(lambda)(nekoConf.network.lambda)
      assertResult(logHandlers)(nekoConf.log.handlers)
      assertResult(logLevel)(nekoConf.log.level)
    }
  }
}
