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
 * Date: 02/06/15
 * Time: 18:56
 *
 */
package tests.init

import neko._
import tests.init.SimpleActiveProtocol.SimpleMessage

class SimpleActiveProtocol(config: ProcessConfig)
  extends ActiveProtocol(config, "App")
{
  override def preStart(): Unit =
  {
    println(s"SimpleActiveProtocol > preStart ($process)")
  }

  listenTo(classOf[SimpleMessage])
  def run (): Unit = {
    val other = me.map(x => (x+1) % N)

    println(s"SimpleActiveProtocol > Hello world ($process)")
    SEND(SimpleMessage(me, other, s"I am $me"))

    Receive {
      case SimpleMessage(_,_,text,_) =>
        //println(s"SimpleActiveProtocol > Receive : SimpleMessage($text) at time ${system.currentTime.asSeconds}")
      case m => println(s"SimpleActiveProtocol > Receive : got something else: $m")
    }
  }

  override def deliver(ev: Event): Unit = {
    println(s"SimpleActiveProtocol > $me @ deliver $ev")
    super.deliver(ev)
  }
}

object SimpleActiveProtocol
{
  case class SimpleMessage(from: PID, to: PID, text: String, id: MessageID = MessageID.auto())
    extends UnicastMessage(from,to,id)
}

class SingleActiveReceiverInitializer extends ProcessInitializer
{
  forProcess { p => p.register(new SimpleActiveProtocol(p)) }
}
