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
 * Algorithm to detect the termination of a distributed computation.
 *
 * The algorithm implemented in this protocol is not restricted to diffusing computations, does not
 * require FIFO channels, but works only on a fully connected network topologyDescriptor (although an
 * adaptation to arbitrary topologyDescriptor is feasible).
 *
 * @param config   the process on which the protocol runs.
 */
class SafraTerminationDetection(config: ProcessConfig)
  extends ReactiveProtocol(config, "Safra termination detection")
{
  import SafraTerminationDetection._
  import util.TerminationDetectionClient._

  private val initiator = PID(0)

  private val nextProcess = me.map { me => (me + N - 1) % N }
  private var msgCount = 0
  private var state : State = Active
  private var color : StateColor = White
  private var token : Option[Token] = if (me == initiator) Some(Token(White, 0)) else None


  private def transmitToken(token: Option[Token]) {
    token match {
      case Some(_) if me == initiator =>
        color = White
        SEND(TokenHolder(me, nextProcess, Token(White, 0)))

      case Some(Token(col, q)) if color == White =>
        SEND(TokenHolder(me, nextProcess, Token(col, msgCount + q)))

      case Some(Token(_, q)) if color == Black =>
        color = White
        SEND(TokenHolder(me, nextProcess, Token(Black, msgCount + q)))

      case _ =>
    }
    this.token = None
  }

  def onSend = {
    case util.DiffusingComputation.Initiate =>
      /* ignore (compatibility w/diffusing computations) */

    case BecomePassive if state == Active =>
      state = Passive
      transmitToken(token)

    case BecomePassive if state == Passive =>
      throw new Exception(s"A process ($me) cannot turn passive when it is already passive.")

    case ie : Signal if state == Active => SEND(ie)

    case m : Message if state == Active =>
      msgCount += m.destinations.size
      SEND(Payload(m))

    case m if state == Passive =>
      throw new Exception(s"A process ($me) cannot send messages while passive: $m")

  }

  listenTo (classOf[Announce])
  listenTo (classOf[Payload])
  listenTo (classOf[TokenHolder])
  def onReceive = {
    case _ : Announce => DELIVER (Terminate)
    case Payload(m) =>
      color = Black
      state = Active
      msgCount -= 1
      DELIVER (m)

    case TokenHolder(_,_,Token(White,q),_) if me == initiator
                                          && (msgCount + q == 0)
                                          && (color == White)
                                          && (state == Passive)
      => SEND(Announce(me,ALL))
    case TokenHolder(_,_,tok,_) if me == initiator && state == Active => token = Some(tok)
    case TokenHolder(_,_,tok,_) if me == initiator  => transmitToken(Some(tok))
    case TokenHolder(_,_,tok,_) if state == Active  => token = Some(tok)
    case TokenHolder(_,_,tok,_) if state == Passive => transmitToken(Some(tok))
  }
}


object SafraTerminationDetection
{
  sealed abstract class StateColor
  case object Black extends StateColor
  case object White extends StateColor

  sealed abstract class State
  case object Active extends State
  case object Passive extends State

  case class Payload(m: Message) extends Wrapper(m)
  case class Announce(from: PID, to: Set[PID], id: MessageID = MessageID.auto()) extends MulticastMessage(from, to, id)

  case class Token (color: StateColor, count: Long)
  case class TokenHolder (from: PID, to: PID, token: Token, id: MessageID = MessageID.auto()) extends UnicastMessage(from, to, id)
  //{ def to = Set(_to) }
}
