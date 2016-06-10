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
 * Date: 21/05/15
 * Time: 19:12
 *
 */
package neko.config

object CF
{
  // ==== Simulation
  val SIMULATION   = "neko.simulation"

  // ==== Process
  val PROCESS_NUM  = "neko.process.num"
  val PROCESS_INIT = "neko.process.initializer"

  // ==== Network
  val NETWORK        = "neko.network.class"
  val NETWORK_LAMBDA = "neko.network.lambda"

  // ==== Logging
  val LOG_HANDLERS = "neko.log.handlers"
  val LOG_LEVEL    = "neko.log.level"
}
