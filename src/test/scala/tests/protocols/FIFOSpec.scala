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
package tests.protocols

import com.typesafe.config.Config
import neko._
import neko.config.{ CF, NekoConfig }
import neko.kernel.Initializer
import neko.kernel.sim.NekoSimSystem
import neko.protocol._
import org.scalatest.FlatSpec


class ProcessApp(c: ProcessConfig, nickname: String) extends ActiveProtocol(c,nickname)
{
  import ProcessApp.AppMessage

  val numIterations = 10

  listenTo(classOf[AppMessage])

  def run (): Unit =
  {
    for (i <- 1 to numIterations) {
      SEND(AppMessage(me, neighbors, i))
    }

    var receipts = Map.empty[PID,Int].withDefaultValue(0)
    for (k <- 1 to numIterations * neighbors.size) {
      Receive {
        case AppMessage(from,_,sn,_) if sn == receipts(from) + 1 =>
          receipts = receipts.updated(from, sn)

        case AppMessage(from,_,sn,_) =>
          println(s"Out-of-sync message: from=$from, sn=$sn; expected=${receipts(from)}")
          assert(false)
      }
    }
    neighbors.foreach { p =>
      assert(receipts(p) == numIterations, s"checking assertion for process $p (last=${receipts(p)})")
    }
  }
}

object ProcessApp
{
  case class AppMessage(from: PID, to: Set[PID], sn: Int, id: MessageID = MessageID.auto())
    extends MulticastMessage(from,to,id)
}

class FIFOInitializer extends ProcessInitializer
{
  forProcess { config =>
    val app  = config.register(new ProcessApp(config, "app"))
    val fifo = config.register(new FIFOChannel(config))
    app --> fifo
  }
}


class FIFOSpec extends FlatSpec
{

  behavior of "FIFOChannel"

  it should "deliver messages in the same order they are sent" in {
    val processNum = 5
    val network  = classOf[neko.network.sim.RandomSimNetwork].getName
    val procInit = classOf[FIFOInitializer].getName

    val config = NekoConfig(
      configFor(
        simulation = true,
        processNum = processNum,
        net = network,
        procInit = procInit,
        lambda = 1.0,
        logHandlers = List("Dummy"),
        logLevel = "debug"
      ),
      neko.topology.Clique
    )

    object MySystem extends NekoSimSystem(config)
    MySystem.mainloop()

  }

  def configFor(
    simulation: Boolean,
    processNum: Int,
    procInit: String,
    net: String,
    lambda: Double,
    logHandlers: List[String],
    logLevel: String
  ): Config =
    Initializer.fromMap(
      Map(
        "neko.simulation" -> simulation,
        CF.PROCESS_NUM  -> processNum,
        CF.PROCESS_INIT -> procInit,
      //
      // ---- The network used for communication.
        CF.NETWORK -> net,
        CF.NETWORK_LAMBDA -> lambda
      //
      // ---- Logging options
//        CF.LOG_HANDLERS -> logHandlers,
//        CF.LOG_LEVEL    -> logLevel
      ))
}
