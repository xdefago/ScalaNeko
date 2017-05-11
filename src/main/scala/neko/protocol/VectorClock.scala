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
 * Support for vector clocks.
 *
 * @param config   the process on which the protocol runs.
 */
class VectorClock (config: ProcessConfig)
  extends ReactiveProtocol(config, "Vector clocks")
    with LogicalClock[IndexedSeq[Long]]
{
  import VectorClock._

  private var vc = IndexedSeq.fill[Long](N)(0)  // N: number of processes

  private def updateClock(f: Function[IndexedSeq[Long], IndexedSeq[Long]]) : IndexedSeq[Long] =
    synchronized {
      val update = f(vc)
      vc = update
      vc
    }

  private def increment(vec: IndexedSeq[Long]): IndexedSeq[Long] =
    vec.indices.map {
      case i if i == me.value => vec(i) + 1
      case i                  => vec(i)
    }

  private def combine(v1: IndexedSeq[Long], v2: IndexedSeq[Long])(f:(Long,Long)=>Long): IndexedSeq[Long] =
  {
    assert (v1.size == v2.size)
    v1.indices.map { i => f(v1(i), v2(i)) }
  }

  def time = vc

  def internal(action: => Unit) = {
    updateClock { vc => increment(vc) }
    action
  }

  def onSend = {
    case ie: Signal =>
      updateClock { vc => increment(vc) }
      SEND(ie)

    case m: Message =>
      updateClock { vc => increment(vc) }
      SEND(VCTimeStamped(m, vc))  // send message to lower layer
  }

  listenTo(classOf[VCTimeStamped])
  def onReceive = {
    case VCTimeStamped(m, ts) =>
      updateClock { vc => combine (increment(vc), ts) ((x,y) => x max y) }
      DELIVER(m)        // deliver message to upper layer
  }
}


object VectorClock
{
  case class VCTimeStamped(m: Message, ts: IndexedSeq[Long]) extends Wrapper(m)
}

