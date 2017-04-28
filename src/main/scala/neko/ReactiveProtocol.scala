/**
 *
 * Copyright 2015 Xavier Defago & Naoyuki Onuki
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
 * Date: 15/06/15
 * Time: 18:54
 *
 */
package neko

import neko.trace.Tracing

import scala.reflect.ClassTag


/**
 * Convenience class to replace [[ProtocolImpl]] with all common traits already mixed in.
 *
 * A protocol that sits as a middle layer between application and network can extend this class,
 * which provides all of the basic functionalities.
 *
 * A subclass must provide an implementation to the methods [[Sending!.onSend]] and
 * [[Receiving!.onReceive]].
 *
 * @param config    configuration from the process to which the protocol instance is attached
 * @param nickname  nickname of the protocol (aimed to be used for io)
 */
abstract class ReactiveProtocol(config : ProcessConfig, nickname : String = "unnamed")
  extends ProtocolImpl(config, nickname)
    with ListenerUtils
    with Sending
    with Receiving
    with Tracing
{
  /**
   * this method does nothing for reactive protocols; take downs should instead override the
   * method [[onShutdown]].
   */
  final override def onFinish(): Unit = {}
  
  /* from here, added for tracing message between protocols */
  override def SEND(m: Event) = {
    tracer.SEND(system.currentTime, me, this)(m)
    super.SEND(m)
  }
  override def DELIVER(m: Event) = {
    val protocols = dispatcher.protocolsFor(m.getClass)
    for (proto <- protocols) {
      tracer.DELIVER(system.currentTime, me, this)(m)
    }
    super.DELIVER(m)
  }
  override def deliver(m: Event) = {
    tracer.deliver(system.currentTime, me, this)(m)
    super.deliver(m)
  }
  override def send(m: Event) = {
    tracer.send(system.currentTime, me, this)(m)
    super.send(m)
  }
  /* until here */

  def setTrace[A: tracer.ru.TypeTag: ClassTag](obj: A, nameOfVariable: String*): Unit = {
    if (nameOfVariable.isEmpty) tracer.setTrace(obj, me, this.toString)
    else nameOfVariable.foreach(str => tracer.setTrace(obj, me, this.toString, name = str))
  }
}
