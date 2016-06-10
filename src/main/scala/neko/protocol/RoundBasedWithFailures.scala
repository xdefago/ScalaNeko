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
 * Date: 12/07/2014
 * Time: 10:38
 *
 */
package neko.protocol

import neko._


class RoundBasedWithFailures(config: ProcessConfig, faultySet: Set[PID]=Set.empty)
  extends ReactiveProtocol(config, "round-based failures")
{
  import RoundBasedWithFailures._

  val rand = new scala.util.Random()

  private var state : State = State.Running
  private var round = 0

  // creates a "private" underlying RoundBased protocol
  private val roundBased = new RoundBased(config)

  super.-->(roundBased)

  override def --> (sender: Sender) = { roundBased --> sender }

  override def preStart() =
  {
    roundBased.preStart()
    checkCrashing()
  }

  private def nextBoolean() = rand.nextBoolean() // && rand.nextBoolean()

  private def checkCrashing() {
    if (faultySet.contains(me) && nextBoolean()) { state = State.Crashing }
  }

  def onSend = {
    case _ if state == State.Crashed => /* DROP */

    case ie : Signal => SEND (ie)

    case m  : Message if state == State.Crashing =>
      state = State.Crashed
      val destShuffle = rand.shuffle(m.destinations.toSeq)
      val lost        = destShuffle.take(destShuffle.size-1)
      val lostSubset  = lost.toSet
      val other  = ALL diff m.destinations
      SEND(PartialCrashed(m, lostSubset))
      if (other.nonEmpty) SEND(Crashed(me, other))

    case m  : Message       => SEND (m)
  }

  listenTo(RoundBased.InitRound.getClass)
  listenTo(classOf[RoundBased.StartRound])

  def onReceive = {
    case ( RoundBased.InitRound | RoundBased.StartRound(_,_) ) if state == State.Crashed =>
      SEND(Crashed(me, ALL))

    case _ if state == State.Crashed  => /* DO NOTHING */

    case RoundBased.InitRound          =>
      checkCrashing() ; DELIVER (InitRound)

    case RoundBased.StartRound(r,msgs) =>
      val batch = msgs.mapValues {
        case Some(_:Crashed)                                         => None
        case Some(PartialCrashed(_,crashed)) if crashed.contains(me) => None
        case Some(PartialCrashed(m,_))                               => Some(m)
        case m => m
      }
      round = r
      checkCrashing() ; DELIVER(StartRound(r,batch))
  }
}


object RoundBasedWithFailures
{
  sealed abstract class State
  object State
  {
    case object Running  extends State
    case object Crashing extends State
    case object Crashed  extends State
  }

  case object InitRound extends Signal
  case class StartRound(round: Int, msgs: Map[PID,Option[Message]]) extends Signal
  case class CRASH(round: Int) extends Signal

  case class PartialCrashed(msg: Message, crashed: Set[PID]) extends Wrapper(msg)
  case class Crashed(from: PID, to: Set[PID], id: MessageID = MessageID.auto())
    extends MulticastMessage(from, to, id)
}
