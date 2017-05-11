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

import neko.kernel.{ Dispatcher, NekoSystem }

/**
 * Core class for implementing a reactive protocol.
 *
 * This provides the basic functionality for implementing reactive protocols.
 * It is designed to be used in conjunction with [[Sending]] and [[Receiving]].
 * It is more convenient to extends the class [[ReactiveProtocol]] which has most relevant traits
 * mixed-in.
 *
 * @param config    configuration from the process to which the protocol instance is attached
 * @param nickname  nickname of the protocol (aimed to be used for io)
 */
abstract class ProtocolImpl(config : ProcessConfig, nickname : String = "unnamed")
  extends Protocol with ProtocolUtils
{
  config.register(this)

  /**
   * identifier of the process to which the protocol is attached.
   * @return identifier of the process
   */
  def process: PID     = config.pid

  //def out: PrintStream = Console.out
  
  /**
   * dispatcher to be used when delivering messages. Typically, this is the default dispatcher of
   * the process.
   * @return dispatcher for message delivery
   */
  def dispatcher: Dispatcher = config.dispatcher

  /**
   * system in which the protocol is running. This gives access to some general information about
   * the system, such as the total number of processes.
   * @return the system in which the protocol runs.
   */
  def system: NekoSystem     = config.system

  /**
   * identifier of the protocol.
   */
  val id = ProtoID(nickname)

  override def toString = s"${process.name}:${id.name}{${super.toString}}"
  
  def eventTracer = config.tracer
  
  eventTracer.register(this)
  
  //eventTracer.protocols = eventTracer.protocols :+ name
  eventTracer.protocolImpls = eventTracer.protocolImpls.updated(me, eventTracer.protocolImpls.getOrElse(me, Seq()) :+ this)
}



