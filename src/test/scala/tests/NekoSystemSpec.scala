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

import com.typesafe.config.Config
import neko.config.{ CF, NekoConfig }
import neko.kernel.Initializer
import neko.kernel.sim.NekoSimSystem
import org.scalatest.flatspec.AnyFlatSpec
import tests.init._

class NekoSystemSpec extends AnyFlatSpec
{
  behavior of "NekoSystem"


  it should "create networks and processes according to the configuration" in {
    val processNum = 5
    val network  = classOf[neko.network.sim.RandomSimNetwork].getName
    val procInit = classOf[DummyProcessInitializer].getName

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

    assertResult(processNum)(MySystem.processNum)
    assertResult(network)(MySystem.network.getClass.getName)
    assertResult(processNum)(MySystem.processIDs.size)
    assertResult(processNum)(MySystem.processes.size)

    MySystem.mainloop()
  }


  it should "create processes and their protocols" in {
    val processNum = 3
    val network  = classOf[neko.network.sim.RandomSimNetwork].getName
    val procInit = classOf[SingleActiveReceiverInitializer].getName

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

    assertResult(processNum)(MySystem.processNum)

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
