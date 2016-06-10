/**
 *
 * Copyright 2015 Xavier Defago
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
 * Date: 30/06/15
 * Time: 13:25
 *
 */
package tests.init

import neko._
import neko.protocol.AnonymousRounds

import scala.collection.mutable


object AnonRound
{
  import Client.{Ballot, RoundInfo}
  import protocol.AnonymousRounds._

  val numRounds = 10

  val aggregator = mutable.Map.empty[PID,List[RoundInfo]].withDefaultValue(Nil)

  object AggregatorLock

  class Client (p: ProcessConfig) extends ReactiveProtocol (p, "client")
  {

    def onSend = { case e: Event => assert (assertion = false, e) }

    listenTo (InitRound.getClass)
    listenTo (classOf[StartRound])

    def onReceive =
    {
      case InitRound =>
        println(s"INIT ${me.name}")
        SEND ( Ballot (me, 0) )

      case StartRound (round, mine, other) =>
        println(s"ROUND($round) ${me.name}")
        val info = RoundInfo(me, round, other.collect{ case b: Ballot => b }.toSet + mine.asInstanceOf[Ballot])
        AggregatorLock.synchronized {
          aggregator(me) = info :: aggregator (me)
          //aggregator = aggregator.updated (me, info :: aggregator (me))
        }
        //println(s"aggregator = $aggregator")

        if (round <= numRounds) SEND ( Ballot (me, round) )
    }
  }


  object Client
  {
    case class Ballot (from: PID, round: Int) extends Anonymous
    case class RoundInfo (from: PID, round: Int, ballots: Set[Ballot])
  }

  class Initializer extends ProcessInitializer
  {
    forProcess{ p =>
      val anon   = new AnonymousRounds(p)
      val client = new Client(p)
      client --> anon
    }
  }
}

