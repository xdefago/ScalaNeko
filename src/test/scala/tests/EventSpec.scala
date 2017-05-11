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
package tests

import neko._
import org.scalatest.FlatSpec

import scala.util.Random


class EventSpec extends FlatSpec
{
  val numIterations = 10
  val randomSeed = 0xcafebabefacebeefL

  def generateHeaderInfo(rand: Random, num: Int): IndexedSeq[(PID, Set[PID])] =
    for {
      i <- 0 until num
      from = PID(rand.nextInt(Int.MaxValue))
      to = for (j <- 0 until rand.nextInt(numIterations)) yield PID(rand.nextInt(Int.MaxValue))
    } yield (from, to.toSet)

  case class MyUnicast  (from: PID, to: PID,      id: MessageID = MessageID.auto()) extends UnicastMessage(from, to, id)
  case class MyMulticast(from: PID, to: Set[PID], id: MessageID = MessageID.auto()) extends MulticastMessage(from, to, id)
  case class MyWrapper(msg: Message, value: String) extends Wrapper(msg)

  behavior of "Signal"

  it should "have unique identifiers generated automatically" in {
    case class MySignal(s: String) extends Signal

    val sigs = for (i <- 1 to numIterations) yield MySignal(s"sig-$i")

    for {
      i <- sigs.indices
      j <- sigs.indices
      if i != j
    } {
      assert(sigs(i).id != sigs(j).id)
    }
  }

  it should "not generate ids for wrapper signals" in {
    case class MySignal(s: String) extends Signal
    case class Wrapper(s: Signal) extends Signal { override lazy val id = s.id }

    val sig = MySignal("sig")
    val wrap = Wrapper(sig)

    assertResult(sig.id)(wrap.id)
  }

  behavior of "MulticastMessage"

  it should "retain from and destinations fields" in {
    val rand = new Random(randomSeed)
    val info = generateHeaderInfo(rand, numIterations)
    val headers =
      for ((from, to) <- info) yield (from, to, MyMulticast(from, to))

    for ((expectedFrom, expectedTo, header) <- headers) {
      assertResult(expectedFrom)(header.from)
      assertResult(expectedTo)(header.destinations)
    }
  }

  it should "have auto-incremented ids" in {
    val from = PID(0)
    val to = Set(PID(1), PID(2))
    val messages = IndexedSeq.tabulate(numIterations+1) { i => MyMulticast(from, to) }
    val firstID  = messages.head.id.value
    val headers  =
      for (i <- messages.indices)
        yield (MessageID(firstID + i), messages(i))

    for ((expectedID, header) <- headers) {
      assertResult(expectedID)(header.id)
      assertResult(from)(header.from)
      assertResult(to)(header.destinations)
    }
  }

  it should "not increment but retain ID upon copy" in {
    val from = PID(0)
    val to = Set(PID(1), PID(2))
    val otherFrom = PID(1000)
    val otherTo = Set(PID(123))
    val headers =
      for (i <- 1 to numIterations)
        yield MyMulticast(from, to)

    for (header <- headers) {
      val expectedID = header.id
      val copyHeader = header.copy(from = otherFrom, to = otherTo)
      assertResult(expectedID)(copyHeader.id)
    }
  }

  behavior of "UnicastMessage"

  it should "retain from and destinations fields" in {
    val rand = new Random(randomSeed)
    val info = generateHeaderInfo(rand, numIterations).filter(p => p._2.nonEmpty)
    val headers =
      for ((from, to) <- info) yield (from, to, MyUnicast(from, to.head))

    for ((expectedFrom, expectedTo, header) <- headers) {
      assertResult(expectedFrom)(header.from)
      assertResult(expectedTo.head)(header.destinations.head)
    }
  }

  it should "have auto-incremented ids" in {
    val from = PID(0)
    val to = Set(PID(1), PID(2))
    val messages = IndexedSeq.tabulate(numIterations+1) { i => MyUnicast(from, to.head) }
    val firstID  = messages.head.id.value
    val headers  =
      for (i <- messages.indices)
        yield (MessageID(firstID + i), messages(i))

    for ((expectedID, header) <- headers) {
      assertResult(expectedID)(header.id)
      assertResult(from)(header.from)
      assertResult(to.head)(header.destinations.head)
    }
  }

  it should "not increment but retain ID upon copy" in {
    val from = PID(0)
    val to = Set(PID(1), PID(2))
    val otherFrom = PID(1000)
    val otherTo = PID(123)
    val headers =
      for (i <- 1 to numIterations)
        yield MyUnicast(from, to.head)

    for (header <- headers) {
      val expectedID = header.id
      val copyHeader = header.copy(from = otherFrom, to = otherTo)
      assertResult(expectedID)(copyHeader.id)
    }
  }

  behavior of "Wrapper"

  it should "inherit attributes from top-level message" in {
    val rand = new Random(randomSeed)
    val info = generateHeaderInfo(rand, numIterations)
    val toplevels =
      for {
        ((from, to), i) <- info.zipWithIndex
        head = MyMulticast(from, to)
        str = s"info$i"
        msg = MyWrapper(head, str)
      } yield (from, to, str, head, msg)

    for ((expectedFrom, expectedTo, expectedStr, head, msg) <- toplevels) {
      val expectedID = head.id
      assertResult(expectedFrom)(head.from)
      assertResult(expectedTo)(head.destinations)
      assertResult(expectedFrom)(msg.from)
      assertResult(expectedTo)(msg.destinations)
      assertResult(expectedID)(msg.id)
      assertResult(expectedStr)(msg.value)
    }
  }

  it should "inherit attributes at each level when chained" in {
    val rand = new Random(randomSeed)
    val info = generateHeaderInfo(rand, numIterations)
    val toplevels =
      for {
        ((from, to), i) <- info.zipWithIndex
        header = MyMulticast(from, to)
        str1 = s"info-A-$i"
        str2 = s"info-B-$i"
        msg = MyWrapper(MyWrapper(header, str1), str2)
      } yield (from, to, str1, str2, header, msg)

    for ((expectedFrom, expectedTo, expectedStr1, expectedStr2, header, msg) <- toplevels) {
      val expectedID = header.id
      val outerMsg = msg
      val innerMsg = msg.msg.asInstanceOf[MyWrapper]

      for (layer <- List(header, innerMsg, outerMsg)) {
        assertResult(expectedFrom)(layer.from)
        assertResult(expectedTo)(layer.destinations)
        assertResult(expectedID)(layer.id)
      }
      assertResult(header)(innerMsg.msg)
      assertResult(expectedStr2)(outerMsg.value)
      assertResult(expectedStr1)(innerMsg.value)
    }
  }
}
