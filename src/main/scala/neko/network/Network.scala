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
package neko.network

import com.typesafe.scalalogging.LazyLogging
import neko._
import neko.kernel.NekoSystem
import neko.trace.Tracing

import scala.util.Try

/**
 * Implements the behavior of a network.
 */
trait Network extends NamedEntity with Sender
{
  def name      = "network"
  def senderOpt = None
  
  def topology: neko.topology.Topology

  /**
   * Sends a new event through the network. If `m` is a message, then it will be transmitted to its
   * destinations. If `m` is a signal, the network will simply drop it, possibly leaving some
   * warning in the logs.
   *
   * @param m the event to be sent by the protocol
   */
  def send(m: Event)

  /**
   * returns a mapping of process identifiers to their corresponding receiver.
   *
   * @return a mapping of process identifiers to their corresponding receiver.
   */
  def receivers: Map[PID, Receiver]

  /**
   * returns the receiver corresponding to the process identifier given in argument, if any.
   * @param pid identifier of a process
   * @return receiver of the given process if found, or `None`.
   */
  def receiverFor(pid: PID): Option[Receiver] = receivers.get(pid)

  /**
   * perform an action on the receiver of a given process, if found.
   *
   * @param pid    identifier of a process
   * @param action action to execute on the receiver of the process
   * @tparam T     return type of the action
   * @return       return value of the action, or `None` if no receiver was found for the process
   */
  def doForProcess[T](pid: PID)(action: Receiver => T): Option[T] = receivers.get(pid).map(action)

  protected[neko] def receivers_=(receivers: Map[PID, Receiver])

  /**
   * hook called before the network is started.
   */
  def preStart(): Unit = {}

  /**
   * hook called upon starting the network.
   */
  def start(): Unit    = {}
}


object Network
{
  /**
   * creates a new instance of [[Network]] from the given class name.
   * @param className name of the subclass of [[Network]] to instantiate.
   * @return `Success` with the new instance if successful, or `Failure` otherwise.
   */
  def forName(className: String): Try[Class[_<:Network]] =
    Try {
      Class
        .forName(className)
        .asSubclass(classOf[Network])
    }
}


/**
 * implements the basic functionality of networks.
 * @param system system in which the network will run.
 */
abstract class AbstractNetwork(val system: NekoSystem)
  extends Network with LazyLogging with Tracing
{
  protected val N = system.processNum

  lazy val topology = system.config.network.topology(N)

  def receivers: Map[PID, Receiver] = _receivers

  private var _receivers = Map.empty[PID, Receiver]

  /**
   * Sets the list of possible destinations for this network.
   *
   * @param receivers mapping of process identifiers to their corresponding receivers.
   */
  def receivers_=(receivers: Map[PID, Receiver]) =
  {
    _receivers = receivers
  }

  /**
   * delivers a message to a given destination process.
   *
   * @param dest process to which `m` should be delivered
   * @param m  message to deliver
   */
  protected[this] def sendTo(dest: PID, m: Message): Unit =
  {
    logger.trace(s"sendTo(${dest.name}, ${m.toPrettyString})")
    if (m.destinations.contains(dest)) {
      receivers
        .get(dest)
        .fold {
          logger.warn(s"Destination ${dest.name} not found for message $m")
        } { p =>
          tracer.DELIVER(system.currentTime, dest, this)(m)
          p.deliver(m)
        }
    } else {
      logger.error(s"Attempt to deliver message to ${dest.name} which is not a destination for message:\n$m")
    }
  }


  def send(event: Event) : Unit =
  {
    logger.trace(s"send($event)")
    event match {
      case e: Signal =>
        logger.debug(s"Dropped signal: $e")

      case m: BroadcastMessage =>
        val neighbors = topology.neighborsFor(m.from).getOrElse(Set.empty)
        for (dest <- neighbors if dest != m.from) { sendTo(dest, m) }
        
      case m: Message =>
        tracer.send(system.currentTime, m.from, this)(m)
        for (dest <- m.destinations) { sendTo(dest, m) }
    }
  }
}
