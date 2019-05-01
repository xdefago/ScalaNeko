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
package tests.init

import neko._
import neko.protocol.AnonymousRounds

import scala.collection.mutable


object AnonRound
{
  import Client.Info
  import protocol.AnonymousRounds._

  val numRounds = 10

  val aggregator = mutable.Map.empty[PID,List[Info]].withDefaultValue(Nil)

  object AggregatorLock

  class Client (p: ProcessConfig) extends ReactiveProtocol (p, "client")
  {
    import Client.{ Ballot, RoundInfo }

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
        }

        if (round <= numRounds) SEND ( Ballot (me, round) )
    }
    
  }


  object Client
  {
    case class Ballot (from: PID, round: Int) extends Anonymous
    sealed trait Info
    case class RoundInfo (from: PID, round: Int, ballots: Set[Ballot]) extends Info
    case class ErrorInfo (from: PID, e: Throwable) extends Info
  }

  class Initializer extends ProcessInitializer
  {
    import Client.ErrorInfo
    
    forProcess{ p =>
      val anon   = new AnonymousRounds(p) {
        override def onError (e: Throwable): Unit = {
          println(s"AnonymousRounds > $me @ onError $e")
          aggregator(me) = ErrorInfo(p.pid, e) :: aggregator(me)
        }
      }
      val client = new Client(p)
      client --> anon
    }
  }
}

