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
package neko

/**
 * Defines the behavior for a protocol that provides the sending of messages as capability.
 *
 * The trait is used to complement the functionality of an instance of [[ProtocolImpl]] by declaring
 * that the instance is capable to send a message through the network. Thus, it is usually used
 * together with [[Receiving]]. For instance,
 *
 * {{{
 *   class FIFOChannel (p: NekoProcess, nickname: String)
 *     extends ProtocolImpl(p, nickname)
 *       with Sending with Receiving
 *   {
 *     def onSend = { case ... }
 *   }
 * }}}
 *
 * There are cases where this trait could be used without [[Receiving]], for instance, by a protocol
 * that would only record what it is sent to, without sending anything through the actual network.
 * This is however a very rare situation, and can safely be ignored as far as the lecture I445 is
 * concerned.
 */
trait Sending extends Sender
{
  /**
   * Used to request the protocol to send a given message.
   *
   * The function translates to a call to [[onSend]] in order to handle the message.
   * @param m  the message to send
   */
  def send(m: Event) { this.synchronized { onSend(m) } }

  /**
   * Implements the behavior of the protocol when sending a message.
   *
   * This must be defined in any concrete subclass.
   * It is defined as a partial function taking a message as input and returning nothing.
   *
   * Typically, the partial function will implement different behavior depending on the type of
   * message being sent. For instance,
   * {{{
   *   def onSend = {
   *     case MutexClient.Request =>
   *       want = true ; myTS = lc.time
   *       SEND(Request(me, ALL-me, myTS))
   *     case MutexClient.Release =>
   *       SEND(Ack(me, pendingAcks))
   *       ackCount = 0 ; want = false ; pendingAcks = Set.empty
   *   }
   * }}}
   * @return
   */
  def onSend : PartialFunction[Event, Unit]

  /**
   * the target of any deliver operations. Typically, this is the dispatcher of the process.
   */
  def receiver : Receiver

  /**
   * provides the message or signal to the application. The message or signal passed as argument
   * will be delivered to any protocol that has registered (e.g., using the method
   * [[ListenerUtils!.listenTo]]) for this type of message/signal to the dispatcher providing
   * in [[receiver]].
   * @param m the message or signal to deliver
   */
  protected[this] def DELIVER(m: Event) { receiver.deliver(m) }
}


/**
 * provides the send operation.
 */
trait Sender extends NamedEntity
{
  /**
   * sends a given message or signal (event).
   * @param m the event to be sent by the protocol
   */
  def send(m: Event)
}
