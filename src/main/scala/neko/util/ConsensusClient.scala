/**
 *
 * Copyright 2014 Xavier Defago
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
 * Date: 02/07/2014
 * Time: 10:27
 *
 */
package neko.util

import neko._

trait ConsensusClient[A]
{ this: Receiving with Listener =>

  def propose(v: A) { SEND (ConsensusClient.Propose(v)) }
  listenTo(classOf[ConsensusClient.Decide[A]])
}

object ConsensusClient
{
  case class Propose[A](v: A) extends Signal
  case class Decide[A] (v: A) extends Signal
}
