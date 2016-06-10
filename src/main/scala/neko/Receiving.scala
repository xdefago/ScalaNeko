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
 * Date: 06/06/2014
 * Time: 23:02
 *
 */
package neko


trait Receiver
{
  def deliver(m: Event)
}


/**
 * declares that the protocol implementing the trait is able to "listen" to messages. Typically,
 * this is used indirectly through [[ListenerUtils]].
 */
trait Listener extends Receiver
{
  /**
   * called by the protocol to declare that it will listen to messages/event of the given type.
   * {{{
   *   class MyProtocol(...) extends ... with ListenerUtils
   *   {
   *     listenTo(classOf[MyMessage])
   *     listenTo(MyAlarm.getClass)
   *     def onReceive = {
   *       case MyMessage(...) => ...
   *       case MyAlarm        => ...
   *     }
   *   }
   *   object MyProtocol
   *   {
   *     case class MyMessage(...) extends UnicastMessage(...)
   *     case object MyAlarm extends Signal
   * }}}
   * @param clazz type of the message/event to listen to
   */
  protected[this] def listenTo(clazz: Class[_ <: Event])
}

trait Receiving extends Receiver
{
  def deliver(m: Event) { this.synchronized { onReceive(m) } }
  def onReceive : PartialFunction[Event, Unit]

  protected[this] def sender : Sender

  /**
   * Sends a message using the sender set in the initializer where the protocol is created.
   * If no sender was explicitly set, the message will be set directly through the network.
   * The destination for the message is defined when the message is created.
   *
   * @param m  the message/signal to send
   */
  def SEND(m: Event) { sender.send(m) }
}
