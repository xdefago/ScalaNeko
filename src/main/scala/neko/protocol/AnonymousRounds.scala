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
 * Date: 22/06/2014
 * Time: 21:20
 *
 */
package neko.protocol

import neko._


/**
 * Support for protocols working in a model of anonymous rounds.
 *
 * @param config   the process on which the protocol runs.
 */
class AnonymousRounds(config: ProcessConfig)
  extends ReactiveProtocol(config, "anonymous rounds")
{
  import AnonymousRounds.{Anonymized, Anonymous}

  // creates a "private" underlying RoundBased protocol
  private val roundBased = new SynchronousRounds(config)

  super.-->(roundBased)

  override def --> (sender: Sender) = { roundBased --> sender }

  override def preStart(): Unit = {
    roundBased.preStart()
    super.preStart()
  }

  def onSend = {
    case a : Anonymous        => SEND(Anonymized(me, ALL, a))
    case AnonymousRounds.Done => SEND(SynchronousRounds.Done)
    case ie: Signal           => SEND(ie)
  }

  listenTo(SynchronousRounds.InitRound.getClass)
  listenTo(classOf[SynchronousRounds.StartRound])

  def onReceive = {
    case SynchronousRounds.InitRound =>
      DELIVER (AnonymousRounds.InitRound)

    case SynchronousRounds.StartRound(round, ms) =>
      if (ms.values.forall(opt => opt.fold(true)(_.isInstanceOf[Anonymized]))) {
        val msgs = ms.mapValues {_.map(_.asInstanceOf[Anonymized])}
        val mine = msgs(me).get.content
        val other = msgs.filterKeys( p => p != me).values.flatten.map(_.content).toSeq
        val shuffled = scala.util.Random.shuffle(other)

        DELIVER(AnonymousRounds.StartRound(round, mine, shuffled))
      }
  }
}


object AnonymousRounds
{
  case object InitRound extends Signal
  case object Done      extends Signal
  case class  StartRound(round: Int, mine: Anonymous, other: Seq[Anonymous]) extends Signal

  abstract class Anonymous extends Signal

  case class Anonymized(from: PID, to: Set[PID], content: Anonymous,
    id: MessageID = MessageID.auto())
    extends MulticastMessage(from, to, id)
}

