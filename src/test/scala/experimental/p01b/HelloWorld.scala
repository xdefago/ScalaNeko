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
package experimental.p01b

import neko._
import neko.topology.Clique


class MyProcess(c: ProcessConfig) extends ActiveProtocol(c)
{
  def run (): Unit =
  {
    println(s"Hello World! ${me.name}")
  }
}


object HelloWorld extends Main(
  topology = Clique(4),
  logLevel=ch.qos.logback.classic.Level.OFF
)(
  ProcessInitializer { p => new MyProcess(p) }
)

