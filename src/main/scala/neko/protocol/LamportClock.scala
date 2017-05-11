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
import neko.util.LogicalClock

/**
 * Support for Lamport clocks.
 *
 * @param config   the process on which the protocol runs.
 */
class LamportClock (config: ProcessConfig)
  extends ReactiveProtocol(config, "Lamport clocks")
  with LogicalClock[Long]
{
  import LamportClock._

  private var lc : Long = 0
  private def update(f: Function[Long, Long]) : Long =
    synchronized {
      val update = f(lc)
      if (update > lc) lc = update
      lc
    }

  def time : Long = lc

  def internal(action: => Unit) = {
    update { l => l + 1 }
    action
  }

  def onSend = {
    case ie: Signal =>
      update { l => l + 1 }
      SEND(ie)

    case m: Message =>
      update { l => l + 1 }
      SEND(LCTimeStamped(m, lc)) // send message to lower layer
  }

  listenTo(classOf[LCTimeStamped])

  def onReceive = {
    case LCTimeStamped(m, ts) =>
      update { l => 1 + (l max ts) }
      DELIVER(m)        // deliver message to upper layer
  }
}


object LamportClock
{
  case class LCTimeStamped (m: Message, ts: Long) extends Wrapper(m)
}
