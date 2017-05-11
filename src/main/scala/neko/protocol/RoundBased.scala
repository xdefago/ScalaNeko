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

import com.typesafe.scalalogging.LazyLogging
import neko._

/**
 * Support for round-based protocols.
 *
 * Round-based protocols start with an internal event [[neko.protocol.RoundBased.InitRound]], during which they
 * send exactly one message to each other process, including themselves. This can be done through
 * a broadcast, or through separate calls to [[neko.Receiving#SEND]].
 *
 * After that, each round starts with an internal event [[neko.protocol.RoundBased.StartRound]], which carries
 * the number of the round, as well as a set of messages (one from each of the processes), which
 * were sent during the previous round.
 *
 * @param config   the process on which the protocol runs.
 */
class RoundBased(config: ProcessConfig)
  extends ReactiveProtocol(config, "round-based")
  with LazyLogging
{
  import RoundBased._

  private val emptyBatch = IndexedSeq.fill[Option[Message]](N){ None }

  private var sendRound = 0
  private var sendBatch = Set.empty[Message]
  private var recvRound = 0
  private var recvBatch = emptyBatch
  private var recvStash = Map.empty[Int, List[Message]].withDefaultValue(Nil)
  private var nextBatch = List.empty[Round]


  override def preStart(): Unit =
  {
    DELIVER (RoundBased.InitRound)
  }


  def onSend = {
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

  listenTo(classOf[Round])
  def onReceive = {
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

    case Round (m, r) =>
      logger.warn(s"Ignored message from older round (r=$r; now=$recvRound): $m")

      /*
    case msg @ Round(_,r) if r >  recvRound => nextBatch = msg :: nextBatch
    case Round(m,r) if r == recvRound =>
      recvBatch = recvBatch.updated(m.from.value, Some(m))
      while (! recvBatch.contains(None)) {
        // DELIVER
        recvRound += 1
        val batch = recvBatch
        val (thisRound, nextRound) = nextBatch.partition(m => m.round == recvRound)
        nextBatch = nextRound
        recvBatch = emptyBatch
        for (rm <- thisRound) {
          recvBatch = recvBatch.updated(rm.from.value, Some(rm.msg))
        }
        DELIVER (StartRound(recvRound, batch))
      }
    case m : Round => throw new Exception(s"p$me: (round=$recvRound) got message for older round (${m.round}): $m")
      */
  }
}


object RoundBased
{
  case object InitRound extends Signal
  case class Round(msg: Message, round: Int) extends Wrapper(msg)
  case class StartRound(round: Int, msgs: Map[PID,Option[Message]]) extends Signal
}
