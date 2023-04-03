/*
 * Copyright 2019 Xavier Défago (Tokyo Institute of Technology)
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
import org.scalatest.flatspec.AnyFlatSpec


object StashedReceiveCollateral
{
  val ITER = 10
  
  object Lock
  
  var collection: Map[PID,List[Int]] = Map.empty
  
  class Stasher(p: ProcessConfig) extends ActiveProtocol(p) with StashedReceive
  {
    listenTo(classOf[Msg])
    def run() : Unit =
    {
      var collected = List.empty[Int]
      for (i <- 1 to ITER) {
        ReceiveOrStash {
          case Msg(_, `i`) => collected :+= i
        }
      }
      Lock.synchronized {
        collection = collection.updated(me, collected)
      }
    }
  }
  
  class Thrower(p: ProcessConfig) extends ActiveProtocol(p)
  {
    def run() : Unit =
    {
      for (i <- ITER to 1 by -1) {
        BROADCAST( Msg(me, i) )
      }
    }
  }
  
  case class Msg(from: PID, num: Int) extends BroadcastMessage
  
  class ThrowerStasherInitializer extends ProcessInitializer {
    forProcess { p =>
      if (p.pid == PID(0)) new Thrower(p) else new Stasher(p)
    }
  }
  
}


class StashedReceiveSpec extends AnyFlatSpec
{
  import StashedReceiveCollateral._
  
  behavior of "StashedReceive"
  
  it should "deliver messages in the same order they are sent" in {
    val processNum = 2
    val network  = classOf[neko.network.sim.ConstantSimNetwork].getName
    val procInit = classOf[ThrowerStasherInitializer].getName

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

    assertResult(collection)(Map.empty)
    
    object MySystem extends NekoSimSystem(config)
    MySystem.mainloop()
    assertResult(collection.size)(processNum-1)
    for {
      pi <- 1 until processNum
      p = PID(pi)
      l = (1 to ITER).toList
    } {
      assert(collection contains p)
      assertResult(collection(p))(l)
    }
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
