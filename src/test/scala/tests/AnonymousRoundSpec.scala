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
 * Date: 29/06/15
 * Time: 23:22
 *
 */


import com.typesafe.config.Config
import neko._
import neko.config.{ CF, NekoConfig }
import neko.kernel.Initializer
import neko.kernel.sim.NekoSimSystem
import org.scalatest.FlatSpec
import tests.init.AnonRound


class AnonymousRoundSpec extends FlatSpec
{
  behavior of "AnonymousRound"

  it should "proceed through successive rounds" in {
    val processNum = 3
    val network  = classOf[neko.network.sim.RandomSimNetwork].getName
    val procInit = classOf[AnonRound.Initializer].getName

    val config = NekoConfig (
      configFor (
        simulation = true,
        processNum = processNum,
        net = network,
        procInit = procInit,
        lambda = 1.0,
        logHandlers = List ("Dummy"),
        logLevel = "debug"
      ),
      neko.topology.Clique
    )

    assert(AnonRound.aggregator.isEmpty)

    val mySystem = new NekoSimSystem (config)

    mySystem.mainloop { endTime =>
      println(s"Simulation finished at ${endTime.asSeconds}")

      val result = AnonRound.AggregatorLock.synchronized { AnonRound.aggregator.clone() }

      val processes = (0 until processNum).map (PID).toSet

      //assert (result.nonEmpty, result)
      /*
      assertResult (processes)(result.keySet)
      for ((pid, rounds) <- result) {
        assertResult (AnonRound.numRounds, rounds)(rounds.size)
      }
      */
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
