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

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }

import com.typesafe.scalalogging.LazyLogging
import neko.kernel.ManagedActivity
import neko.trace.Tracing
import neko.util.Time

import scala.reflect.ClassTag

/**
 * Basic class for defining active protocols; those typically at the top of the protocol stack.
 *
 * An active protocol must redefine the [[ActiveProtocol!.run]] method to implement the code of the protocol there.
 * The code in run() can use several convenience methods, such as
 * [[ActiveProtocol!.SEND]], Receive, [[ProtocolUtils.me]], [[ProtocolUtils.N]], [[ProtocolUtils.ALL]].

 * @param config    configutation information from the NekoProcess on which the ActiveProtocol is instantiated
 * @param nickname  some nickname to describe the protocol
 */
abstract class ActiveProtocol(config: ProcessConfig, nickname: String = "unnamed")
  extends ProtocolImpl(config, nickname)
    with ManagedActivity
    with ProtocolUtils
    with Receiving
    with ListenerUtils
    with Runnable
    with LazyLogging
    with Tracing
{
  /**
   * Basic functionality for receiving together with pattern matching.
   *
   * Receiving is done either through [[Receive.apply]] or with [[Receive.withTimeout]].
   * Typical example:
   * {{{
   *   Receive {
   *     case Request(from, to, req) if (to-me).nonEmpty =>
   *       SEND(Request(me, to-me, req))
   *       SEND(Ack(me, from))
   *     case Ack(from, _) => countAcks += 1
   *   }
   * }}}
   *
   * or with a timeout:
   * {{{
   *   Receive.withTimeout(10.0) {
   *     case Request(from, to, req) if (to-me).nonEmpty =>
   *       SEND(Request(me, to-me, req))
   *       SEND(Ack(me, from))
   *     case Ack(from, _) => countAcks += 1
   *     case Timeout => SEND(Suspect(me, ALL-me))
   *   }
   * }}}
   */
  object Receive
  {
    /**
     * Receives and handles the next message in the queue, if any.
     *
     * If the receive queue is empty, the call blocks until a message arrives. When a message
     * is available, it is removed from the queue, and passed to a handler function (can also be
     * a partial function).
     *
     * Typical example:
     * {{{
     *   Receive {
     *     case Request(from, to, req, _) if (to-me).nonEmpty =>
     *       SEND(Request(me, to-me, req))
     *       SEND(Ack(me, from))
     *     case Ack(from, _, _) => countAcks += 1
     *   }
     * }}}
     *
     * Another use is to pass an anonymous function as the handler:
     * {{{
     *   Receive { m =>
     *     println(m)
     *     // other code pertaining to m
     *   }
     * }}}
     *
     * @param handler  function applied upon receiving a message. It can be a partial function.
     * @return the value returned by the handler
     */
    def apply(handler: Function[Event, Unit]) : Unit = {
      val m = RECEIVE()
      handler(m)
    }

    /**
     * Receives and handles the next message in the queue, if any, or times out.
     *
     * If the receive queue is empty, the call blocks until a message arrives or the call times out,
     * whichever occurs first. If a message is available before the timeout, it is removed from the
     * queue, and passed as argument to a handler function (which can also be a partial function).
     * If the timeout occurs before a message is received, the internal event [[Timeout]] is passed
     * instead.
     *
     * Typical example:
     * {{{
     *   Receive.withTimeout(Time.second) {
     *     case Request(from, to, req, _) if (to-me).nonEmpty =>
     *       SEND(Request(me, to-me, req))
     *       SEND(Ack(me, from))
     *     case Ack(from, _, _) => countAcks += 1
     *     case Timeout => SEND(Suspect(me, neighbors))
     *   }
     * }}}
     * @param timeout duration after which a [[Timeout]] is issued
     * @param handler function applied upon receiving a message. It can be a partial function.
     */
    def withTimeout(timeout: Time)(handler: Function[Event, Unit]) : Unit = {
      val m = RECEIVE(timeout)
      handler(m)
    }
  }

  /**
   * Returns the next message in the receive queue, if any.
   * If the receive queue is empty, the call blocks until a message arrives.
   *
   * @return a message
   */
  def RECEIVE() = receive()

  /**
   * Returns the next message in the receive queue, if any, or times out.
   *
   * If the receive queue is empty, the call blocks until a message arrives or the call times out,
   * whichever occurs first.
   *
   * @param withTimeout maximal duration until a timeout occurs.
   * @return A message if it receives one before the timeout, or [[Timeout]] if the timeout occurs.
   */
  def RECEIVE(withTimeout: Time) : Event = receive(withTimeout).getOrElse(Timeout)

  //override def SEND(m: Event):Unit =  { sender.send(m) }

  override def deliver(event: Event): Unit = {
    onReceiveWithTrace.applyOrElse(event, { e: Event =>
      logger.trace(s"${id.name} : ENQUEUE ${e.toPrettyString}")
      messageCount.incrementAndGet()
      blockingQueue.offer(e)
    })
  }

  private def onReceiveWithTrace = {
     val pf: PartialFunction[Event, Event] = {
       case e: Event if onReceive.isDefinedAt(e) =>
         tracer.deliver(system.currentTime, me, this)(e)
         e
     }
     pf.andThen(onReceive)
   }
  
  def onReceive = PartialFunction.empty[Event,Unit]

  // RECEIVE EVENT QUEUE
  private val blockingQueue: BlockingQueue[Event] = new LinkedBlockingQueue()
  private val messageCount = new java.util.concurrent.atomic.AtomicInteger(0)

  protected[neko] def hasPendingMessages: Boolean = messageCount.get() > 0

  def receive(): Event = {
    logger.trace(s"${id.name} : receive()")
    while(blockingQueue.isEmpty) {
      this.willWait()
    }
    logger.debug(s"${id.name} : receive() has event:")
    val ev = blockingQueue.take()
    messageCount.decrementAndGet()
    logger.debug(s"${id.name} : receive() -> ${ev.toPrettyString}")
    /* from here, added for tracing message between protocols */
    tracer.deliver(system.currentTime, me, this)(ev)
    /* until here */
    ev
  }


  def receive(withTimeout: Time): Option[Event] = {
    logger.trace(s"receive($withTimeout)")
    val deadline = system.currentTime + withTimeout

    val wakeupTask = system.timer.scheduleAt(deadline){ t => /* wake up */ }

    while (blockingQueue.isEmpty && system.currentTime < deadline) {
      this.willWait()
    }

    system.timer.cancel(wakeupTask)

    if (blockingQueue.isEmpty) {
      logger.debug(s"receive(deadline=${deadline.asSeconds}) timeout at ${system.currentTime.asSeconds}")
      None
    } else {
      val ev = blockingQueue.take()
      messageCount.decrementAndGet()
      logger.debug(s"receive($withTimeout) at ${system.currentTime.asSeconds} got $ev")
      /* from here, added for tracing message between protocols */
      tracer.deliver(system.currentTime, me, this)(ev)
      /* until here */
      Some(ev)
    }
  }


  def sleep(duration: Time): Unit = {
    if (duration > Time(0)) {
      val deadline = system.currentTime + duration

      val wakeupTask = system.timer.scheduleAt(deadline){ t => /* wake up */ }

      logger.trace(s"${id.name} : SLEEP for ${duration.asNanoseconds}ns until ${deadline.asSeconds}s")
      while (system.currentTime < deadline) {
        this.willWait()
      }

      system.timer.cancel(wakeupTask)
    }
  }

  /* from here, added for tracing message between protocols */
  override def SEND(m: Event) = {
    tracer.SEND(system.currentTime, me, this)(m)
    super.SEND(m)
  }
  /* until here */

  def setTrace[A: tracer.ru.TypeTag: ClassTag](obj: A, nameOfVariable: String*): Unit = {
    if (nameOfVariable.isEmpty)
      tracer.setTrace(obj, me, this.toString)
    else
      nameOfVariable.foreach(str => tracer.setTrace(obj, me, this.toString, name = str))
  }
}

