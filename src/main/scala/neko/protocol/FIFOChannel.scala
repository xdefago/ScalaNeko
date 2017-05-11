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
package neko.protocol

import neko._

import scala.collection.mutable


/**
 * Implements multi-party FIFO channels.
 *
 * The protocol ensures that all messages sent from a process p to a process q are delivered by q
 * in the same order they were sent by p.
 *
 * The protocol is not thread-safe if each process has more than one send thread
 * or more than one receive thread. Fortunately, this is the case as long as each
 * [[neko.NekoProcess]] has only one protocol [[neko.ActiveProtocol]] and doesn't use
 * timers, which is the most common case for examples used in the lecture I445. A real-world
 * implementation would however need to be made thread-safe.
 *
 * @param config   the process on which the protocol runs.
 */
class FIFOChannel (config: ProcessConfig)
  extends ReactiveProtocol(config, "FIFO channels")
{
  import FIFOChannel._

  private val nextSendTo      = Array.fill[Long](N)(0)
  private val nextReceiveFrom = Array.fill[Long](N)(0)
  private val messageBuffer   = IndexedSeq.fill(N)(mutable.PriorityQueue.empty[FIFOInfo](FIFOChannel.FIFOOrdering))

  def onSend = {
    case ie: Signal  => SEND(ie)
    case m : Message =>
      m.destinations foreach {
        dest =>
          val sn = nextSendTo(dest.value)
          nextSendTo(dest.value) += 1
          SEND(FIFOSequenced(m, dest, sn))
      }
  }

  listenTo(classOf[FIFOSequenced])

  def onReceive = {
    case FIFOSequenced(m, dest, sn) =>
      // buffer message
      messageBuffer(m.from.value) enqueue FIFOInfo(m.from, sn, m)

      // deliver pending messages
      val buffer = messageBuffer(m.from.value)
      while (buffer.nonEmpty && buffer.head.seqNum == nextReceiveFrom(m.from.value)) {
        val info = buffer.dequeue()
        DELIVER(info.msg)
        nextReceiveFrom(m.from.value) += 1
      }
  }
}


object FIFOChannel
{
  case class FIFOSequenced(m: Message, to: PID, seqNum: Long) extends Wrapper(m)
  {
    override def destinations = Set(to)
  }

  case class FIFOInfo (from: PID, seqNum: Long, msg: Message)

  val FIFOOrdering = Ordering[(Long, Int)].on((info: FIFOInfo) => (-info.seqNum, info.from.value))
}
