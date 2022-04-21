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

package experimental.snapshot

/**
 * Created by defago on 02/05/2017.
 */

import neko._
import neko.util.SnapshotClient

import scala.util.Random

class ChandyLamport(p: ProcessConfig) extends ReactiveProtocol(p, "Chandy-Lamport Snapshot")
{
  import ChandyLamport._

  private var color : StateColor = White
  private var record        = Set.empty[Message]
  private var snapshotCount = neighbors.size-1

  private def recordMessage(m: Message) = record += m
  private def recordState()             = DELIVER(SnapshotClient.SaveState)

  def onSend = {
    case SnapshotClient.Initiate if color == White => /* initiator */
      color = Red                   // turn red
      recordState()                 // record own state
      SEND(Snapshot(me, neighbors)) // send marker to all channels

    case SnapshotClient.Initiate => /* ignore (already started) */

    case m : Message => SEND(Payload(m, color))
  }

  listenTo(classOf[Payload])
  listenTo(classOf[Snapshot])
  def onReceive = {
    case Payload(m, White) if color == Red => /* red process receives white message */
      recordMessage(m)
      DELIVER(m)

    case Payload(m, _) => DELIVER(m)

    case Snapshot(from,_) => /* receive marker first time */
      if (color == White) {
        color = Red                     // turn red
        recordState ()                  // record own state
        SEND (Snapshot (me, neighbors)) // send marker to all channels
      }
      snapshotCount -= 1
      // Termination: when received marker from all processes -> DELIVER(SnapshotDone).
       if (snapshotCount == 0) { DELIVER(SnapshotClient.SnapshotDone(record)) }
  }
}

object ChandyLamport
{
  sealed abstract class StateColor
  case object Red extends StateColor
  case object White extends StateColor

  case class Payload(msg: Message, color: StateColor) extends Wrapper(msg)
  case class Snapshot(from: PID, to: Set[PID])
    extends MulticastMessage
}


class MultiTokenRotation(p: ProcessConfig, maxTokens: Int = 10)
  extends ActiveProtocol(p, "Multi-tokens")
  with SnapshotClient
{
  import MultiTokenRotation._
  import util.Time

  val initiator = PID(0)
  val rand = new Random()
//  val startingTokens = rand.nextInt(maxTokens) + (if (me==0) 1 else 0)
  val startingTokens = maxTokens
  val nextProcess = me.map(i=>(i + 1) % N)

  var tokenSet : Set[TokenID] = (0 until startingTokens).map(TokenID(me, _)).toSet

  private def forwardToken(tok: TokenID) = Token(me, nextProcess, tok)

  listenTo(classOf[Token])

  override def run(): Unit = {
    for (tok <- tokenSet) {
      SEND(forwardToken(tok))
      tokenSet -= tok
      sleep(Time.millisecond * rand.nextDouble())
    }

    if (me == PID(0) || true) {
      // delayed execution
      system.timer.scheduleAfter (Time.second * -50*math.log (rand.nextDouble ())) {
        t =>
          println(s"${me.name}: INITIATE (at ${t.asSeconds})")
          initiate()
      }
    }

    // generate periodic Tick signals
    case class Tick(time: Time) extends Signal
    val alarmTask = system.timer.periodically(Time.second){
      t =>
        deliver(Tick(t))
        true // true: always reschedule
    }

    while (true) {
      Receive {
        case Tick(t) =>
          //println(s"         ${me.name}: tokens = " + tokenSet.mkString("{",",","}"))
          val (candidates, remainder) = tokenSet.partition{ _ => rand.nextDouble() < 0.5 }
          tokenSet = remainder
          candidates.foreach{ tok => SEND(forwardToken(tok)) }

        case Token(_,_,tok) if rand.nextDouble() < 0.2 => SEND(forwardToken(tok))
        case Token(_,_,tok) => tokenSet += tok

        case SnapshotClient.SaveState         =>
          println(s"${me.name}: SAVE STATE "+tokenSet)

        case SnapshotClient.SnapshotDone(rec) =>
          val tokens = rec.collect { case Token(_,_,tok) => tok }
          println(s"${me.name}: SNAPSHOT DONE -> " + tokens)
          system.timer.cancel(alarmTask)
          return

        case _ => /* ignore */
      }
    }
  }
}


object MultiTokenRotation
{
  case class TokenID(process: PID, rank: Int)
  case class Token(from: PID, to: PID, token: TokenID) extends UnicastMessage
}


object ChandyLamportMain extends gui.GUIMain(topology.Clique(3)) (
  ProcessInitializer { p =>
    val app  = new MultiTokenRotation(p, maxTokens = 1)
    val snap = new ChandyLamport(p)
    val fifo = new protocol.FIFOChannel(p)
  
    app  --> snap
    snap --> fifo
  }
)
