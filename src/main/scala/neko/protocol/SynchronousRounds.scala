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

class SynchronousRounds(config: ProcessConfig)
  extends ActiveProtocol(config, "round-based") with Sending
{
  import SynchronousRounds._

  private var sendRound = 0
  private var sendBatch = Set.empty[Message]
  private var recvRound = 0
  private var recvStash = Map.empty[Int, Seq[Message]].withDefaultValue(Nil)
  private var isDone    = false

  listenTo(classOf[SynchronousRounds.Round])

  def run(): Unit =
  {
    DELIVER (InitRound)

    while (! isDone) {
      Receive {
        case Round (msg, r) if r >= recvRound =>
          recvStash = recvStash.updated (r, msg +: recvStash (r))

          val batch = recvStash (recvRound)
          if (batch.size == N) {
            // DELIVER
            recvStash = recvStash - recvRound // remove batch from the stash
            recvRound += 1
            val messageVector = batch.map (m => m.from -> Some (m)).toMap
            DELIVER (StartRound (recvRound, messageVector))
          }

        case m: Round =>
          val round = m.round
          throw new Exception (
            s"p$me: (round=$recvRound) got message for older round ($round): $m"
          )

        case _ => /* ignore */
      }
    }
  }

  def onSend = {
    case Done => isDone = true

    case ie : Signal => SEND (ie)

    case m : Message =>
      if (sendBatch.exists(_.destinations.intersect(m.destinations).nonEmpty)) {
        throw new Exception(s"${me.name} attempts to send several messages to the same process in the same round.")
      }
      sendBatch += m
      val batchSize = sendBatch.foldLeft(0)((a,m)=>a+m.destinations.size)
      if (batchSize == N) {
        val batch = sendBatch
        val round = sendRound
        sendBatch = Set.empty[Message]
        sendRound += 1
        for (m <- batch) { SEND (Round(m, round)) }
      }
  }
}


object SynchronousRounds
{
  case object InitRound extends Signal
  case object Done      extends Signal
  case class Round(msg: Message, round: Int) extends Wrapper(msg)
  case class StartRound(round: Int, msgs: Map[PID,Option[Message]]) extends Signal
}
