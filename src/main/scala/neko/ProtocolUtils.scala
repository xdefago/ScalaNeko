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


import scala.language.reflectiveCalls


/**
 * mixin trait to provide support to listening to a message type.
 */
trait ListenerUtils extends Listener
{ this: { def dispatcher : neko.kernel.Dispatcher } =>
  protected[this] def listenTo(clazz: Class[_ <: Event])
  {
    dispatcher.registerFor(clazz, this)
  }
}


/**
 * Provides basic functionality to the protocol that implements this trait.
 *
 * The concrete protocol must provide an implementation for
 * `process`, `system`, and `ProtocolUtils#dispatcher`.
 * It provides the ability to connect and the following fields:
 *
 *  - `me` is the identifier of the current process.
 *  - `N` is the total number of processes in the system.
 *  - `ALL` is the set of identifiers for all processes in the system (including `me`).
 *  - `neighbors` is the set of identifiers of all neighbor processes.
 */
trait ProtocolUtils
{ this: ProtocolImpl =>
/*{
    def process: PID
    def system: NekoSystem
    def dispatcher: Dispatcher
  } =>
  */
  
  /**
   * identifier of the process in which the protocol is running.
   */
  protected[this] lazy val me  = process

  /**
   * total number of processes running in the system.
   */
  protected[this] lazy val N   = system.processNum

  /**
   * set of all processes in the system.
   */
  protected[this] lazy val ALL = system.processSet

  /**
   * set of processes that are direct neighbors to the process in which this instance of the
   * protocol runs.
   */
  protected[this] lazy val neighbors : Set[PID] = // system.processes.get(process).map(p => p.neighbors).get
    system.network.topology.neighborsFor(me).getOrElse(Set.empty)

  /**
   * receiver to which this protocol delivers messages. Typically, this is the message dispatcher
   * of the process.
   *
   * @return receiver to which messages are delivered
   */
  def receiver : Receiver = _receiver

  /**
   * sets a new receiver to which this protocol will deliver messages. Normally, there is no need
   * for the application programmer to use this, as the receiver is automatically set to a
   * meaningful default value.
   *
   * @param receiver new receiver to which messages will be delivered
   */
  def receiver_= (receiver: Receiver) { this._receiver = receiver }

  /**
   * target sender that will handle [[Sending#send]] operations for this protocol.
   * @return target sender
   */
  def sender : Sender = _sender

  def senderOpt: Option[Sender] = Some(sender)
  
  /**
   * Connects a new sender to this protocol.
   *
   * @param sender the new sender to connect.
   */
  def --> (sender: Sender) { this._sender = sender }


  private var _sender : Sender     = system.network
  private var _receiver : Receiver = dispatcher
}
