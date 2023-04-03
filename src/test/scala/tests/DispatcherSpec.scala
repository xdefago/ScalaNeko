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
import neko.kernel.Dispatcher
import org.scalatest.flatspec.AnyFlatSpec
import scala.annotation.nowarn

class DispatcherSpec extends AnyFlatSpec
{
  val numIterations = 10

  case class DummyMessageA(from: PID, to: Set[PID]) extends MulticastMessage
  case class DummyMessageB(from: PID, to: PID) extends UnicastMessage
  case class DummyMessageC(from: PID, to: Set[PID]) extends MulticastMessage

  case class DummyReceiver(giveEvent: (Event)=>Unit) extends Receiver
  {
    override def deliver (m: Event) = { giveEvent(m) }
  }

  behavior of "Dispatcher"

  it should "register and unregister Receivers properly" in {
    val disp = Dispatcher.withClassLookup()
    val receivers = for (i <- 1 to numIterations) yield DummyReceiver { ev => i: @nowarn } // NB: not using the i results in a single receiver being created but scala warns that the variable is unused
    val msgClasses = IndexedSeq(classOf[DummyMessageA], classOf[DummyMessageB], classOf[DummyMessageC])
    val expectedCount = IndexedSeq(4, 7, 10) // NB: not generic !!

    for (msgClass <- msgClasses) {
      assertResult(Seq(disp.orphanEventHandler))(disp.protocolsFor(msgClass))
    }

    for {
      i <- receivers.indices
      j <- msgClasses.indices
      if i <= receivers.size * (j+1) / msgClasses.size
      recv = receivers(i)
      msgClass = msgClasses(j)
    }{
      disp.registerFor(msgClass, recv)
    }

    for {
      j <- msgClasses.indices
      msgClass = msgClasses(j)
      expCount = expectedCount(j)
    }{
      val protocols = disp.protocolsFor(msgClass)
      assertResult(expCount)(protocols.size)
    }
  }

  it should "dispatch messages only to registered receivers" in {
    var result1: Option[Event] = None
    var result2: Option[Event] = None
    var result3: Option[Event] = None
    val receiver1 = DummyReceiver{ ev => result1 = Some(ev) }
    val receiver2 = DummyReceiver{ ev => result2 = Some(ev) }
    val receiver3 = DummyReceiver{ ev => result3 = Some(ev) }

    val eventA = DummyMessageA(PID(0), Set.empty)
    val eventB = DummyMessageB(PID(0), PID(1))

    val disp = Dispatcher.withClassLookup()

    disp.deliver(eventA)
    assertResult(None)(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)

    disp.registerFor(classOf[DummyMessageA], receiver1)
    disp.registerFor(classOf[DummyMessageB], receiver1)
    disp.registerFor(classOf[DummyMessageC], receiver1)
    disp.registerFor(classOf[DummyMessageB], receiver2)
    disp.registerFor(classOf[DummyMessageC], receiver2)
    disp.registerFor(classOf[DummyMessageC], receiver3)

    disp.deliver(eventA)
    assertResult(Some(eventA))(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)

    disp.deliver(eventB)
    assertResult(Some(eventB))(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)

    result1 = None
    result2 = None
    result3 = None
    disp.unregisterFor(classOf[DummyMessageB], receiver1)

    disp.deliver(eventB)
    assertResult(None)(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)

    disp.deliver(eventA)
    assertResult(Some(eventA))(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)

    disp.registerFor(classOf[DummyMessageA], receiver1)
    disp.registerFor(classOf[DummyMessageA], receiver2)
    disp.registerFor(classOf[DummyMessageA], receiver3)

    disp.deliver(eventA)
    assertResult(Some(eventA))(result1)
    assertResult(Some(eventA))(result2)
    assertResult(Some(eventA))(result3)

    result1 = None
    result2 = None
    result3 = None
    disp.unregisterAllFor(classOf[DummyMessageA])

    disp.deliver(eventA)
    assertResult(None)(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)

    disp.deliver(eventB)
    assertResult(None)(result1)
    assertResult(Some(eventB))(result2)
    assertResult(None)(result3)

    assertResult(Seq(disp.orphanEventHandler))(disp.protocolsFor(classOf[DummyMessageA]))
    assertResult(Seq(receiver2))(disp.protocolsFor(classOf[DummyMessageB]))
    assertResult(Seq(receiver3, receiver2, receiver1))(disp.protocolsFor(classOf[DummyMessageC]))
  }

  case object SignalA extends Signal
  case object SignalB extends Signal

  it should "dispatch case objects properly" in {
    var result1: Option[Event] = None
    var result2: Option[Event] = None

    val receiver1 = DummyReceiver{ ev => result1 = Some(ev) }
    val receiver2 = DummyReceiver{ ev => result2 = Some(ev) }

    val disp = Dispatcher.withClassLookup()

    disp.registerFor(SignalA.getClass, receiver1)
    disp.registerFor(SignalB.getClass, receiver2)

    disp.deliver(SignalA)
    assertResult(Some(SignalA))(result1)
    assertResult(None)(result2)

    disp.deliver(SignalB)
    assertResult(Some(SignalA))(result1)
    assertResult(Some(SignalB))(result2)

    assertResult(Seq(receiver1))(disp.protocolsFor(SignalA.getClass))
    assertResult(Seq(receiver2))(disp.protocolsFor(SignalB.getClass))
  }

  it should "be UNABLE to match subclasses of messages" in {
    abstract class DummyContentBase extends MulticastMessage
    case class DummyContentSubclassA(from: PID, to: Set[PID], info: String)
      extends DummyContentBase
    case class DummyContentSubclassB(from: PID, to: Set[PID], value: Int)
      extends DummyContentBase

    var result1: Option[Event] = None
    var result2: Option[Event] = None
    var result3: Option[Event] = None
    val receiver1 = DummyReceiver{ ev => result1 = Some(ev) }
    val receiver2 = DummyReceiver{ ev => result2 = Some(ev) }
    val receiver3 = DummyReceiver{ ev => result3 = Some(ev) }

    val messageA = DummyContentSubclassA(PID(0), Set.empty, "msgA")
    val messageB = DummyContentSubclassB(PID(1), Set.empty, 10)
    val messageC = DummyMessageA(PID(2), Set.empty)

    val disp = Dispatcher.withClassLookup()
    disp.registerFor(classOf[DummyContentBase], receiver1)
    disp.registerFor(classOf[DummyContentSubclassA], receiver2)
    disp.registerFor(classOf[DummyContentSubclassB], receiver3)

    disp.deliver(messageC)
    assertResult(None)(result1)
    assertResult(None)(result2)
    assertResult(None)(result3)

    disp.deliver(messageB)
    assertResult(None)(result1)
    assertResult(None)(result2)
    assertResult(Some(messageB))(result3)

    disp.deliver(messageA)
    assertResult(None)(result1)
    assertResult(Some(messageA))(result2)
    assertResult(Some(messageB))(result3)
  }
}
