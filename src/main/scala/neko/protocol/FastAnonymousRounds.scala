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

/**
 * Support for protocols working in a model of anonymous rounds.
 *
 * The relevant signals are as follows:
 *
 *   - [[FastAnonymousRounds#InitRound]] is sent once at the beginning to start the initial round.
 *   - [[FastAnonymousRounds#StartRound]] is sent at the beginning of each round and indicates:
 *     - the round number
 *     - the information sent by the processes in the previous round. To distinguish the information
 *       sent by "me", a protocol must keep it in its own state.
 *   - [[FastAnonymousRounds#Done]] is sent by a protocol to indicate that the computation is
 *     finished.
 *
 * @param config   the process on which the protocol runs.
 */
class FastAnonymousRounds(config: ProcessConfig)
  extends ReactiveProtocol(config, "fast anonymous rounds")
{
  import FastAnonymousRounds._

  val coordinator = ALL.max

  var roundNum = 0
  var anonList = List.empty[Anonymous]

  var sendRound = 0


  override def preStart(): Unit = {
    system.timer.scheduleAt(util.Time.ZERO) { t => DELIVER(InitRound) }
  }

  def onSend = {
    case a : Anonymous =>
      SEND(RoundPropose(me, coordinator, a, sendRound))
    case Done          =>
    case ie: Signal    => SEND(ie)
  }

  listenTo(classOf[RoundPropose])
  listenTo(classOf[Round])

  def onReceive = {
    case RoundPropose(_,_,a,r) if me == coordinator && r == roundNum =>
      anonList = a :: anonList
      if (anonList.size == N) {
        roundNum += 1
        SEND(Round(me,ALL,anonList,roundNum))
        anonList = Nil
      }

    case Round(_,_,data,r) if r == sendRound + 1 =>
      sendRound = r
      DELIVER(StartRound(r, data))

  }
}


object FastAnonymousRounds
{
  case object InitRound extends Signal
  case object Done      extends Signal
  case class  StartRound(round: Int, all: Seq[Anonymous]) extends Signal

  abstract class Anonymous extends Signal

  case class Round(from: PID, to: Set[PID], anons: Seq[Anonymous], round: Int)
    extends MulticastMessage

  case class RoundPropose(from: PID, to: PID, content: Anonymous, round: Int)
    extends UnicastMessage
}

