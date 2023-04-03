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
import org.scalatest.flatspec.AnyFlatSpec

class IDSpec extends AnyFlatSpec
{
  behavior of "PID"

  val samplesPID = List(
    (PID(0), 0, "p0"),
    (PID(4), 4, "p4"),
    (PID(6), 6, "p6"),
    (PID(10), 10, "p10"),
    (PID(1012334), 1012334, "p1012334")
  )

  val operationsPID = List[(Int,Int)=>Int](math.min, math.max, _+_, _-_, _*_)

  it should "reply correctly to accessors: id, name, value" in {
    for ((pid, value, name) <- samplesPID) {
      assertResult(value)(pid.value)
      assertResult(name)(pid.name)
    }
  }

  it should "return itself when mapped to identity" in {
    for ((pid, _, _) <- samplesPID) {
      val mappedPID = pid.map(identity)
      assert(mappedPID eq pid)
    }
  }

  it should "return correct values when mapped to addition" in {
    def addFive (x: Int) = x + 5
    for ((pid, value, _) <- samplesPID) {
      val expected = PID(addFive(pid.value))
      val actual = pid.map(addFive)
      assertResult(expected)(actual)
    }
  }

  it should "behave correctly with map2" in {
    val base = PID(123)
    for (
      (pid, value, _) <- samplesPID;
      op <- operationsPID
    ) {
      val expected = PID(op(pid.value, base.value))
      val actual   = pid.map2(base)(op)
      assertResult(expected)(actual)
    }
  }

  behavior of "ProtoID"

  def samplesFor (s: String) = (ProtoID(s), s, s"πρ[$s]")

  val samplesProtoID = List(
    samplesFor(""),
    samplesFor("xavier"),
    samplesFor("⨕⊙∂"),
    samplesFor("x a v"),
    samplesFor("金沢〜板橋"),
    samplesFor("\uD83D\uDE00")
  )

  val operationsProtoID = List[(String,String)=>String]((s1,s2)=>s1, (s1,s2)=>s2, _+_, (s1,s2)=>s2+s1)

  it should "reply correctly to accessors: id, name, value" in {
    for ((id, value, name) <- samplesProtoID) {
      assertResult(value)(id.value)
      assertResult(name)(id.name)
    }
  }

  it should "return itself when mapped to identity" in {
    for ((id, _, _) <- samplesProtoID) {
      val mapped = id.map(identity)
      assert(mapped eq id)
    }
  }

  it should "return correct values when mapped with concatenation" in {
    def addBraces (s: String) = "{" + s + "}"
    for ((id, value, _) <- samplesProtoID) {
      val expected = ProtoID(addBraces(id.value))
      val actual = id.map(addBraces)
      assertResult(expected)(actual)
    }
  }

  it should "behave correctly with map2" in {
    val base = ProtoID(" Something-")
    for (
      (id, value, _) <- samplesProtoID;
      op <- operationsProtoID
    ) {
      val expected = ProtoID(op(id.value, base.value))
      val actual   = id.map2(base)(op)
      assertResult(expected)(actual)
    }
  }

  behavior of "MessageID"

  val samplesMessageID = List(
    (MessageID(0), 0, "m0"),
    (MessageID(4), 4, "m4"),
    (MessageID(6), 6, "m6"),
    (MessageID(10), 10, "m10"),
    (MessageID(1012334), 1012334, "m1012334")
  )

  val operationsMessageID = List[(Long,Long)=>Long](math.min, math.max, _+_, _-_, _*_)

  it should "reply correctly to accessors: name, value" in {
    for ((mid, value, name) <- samplesMessageID) {
      assertResult(value)(mid.value)
      assertResult(name)(mid.name)
    }
  }

  it should "return itself when mapped to identity" in {
    for ((mid, _, _) <- samplesMessageID) {
      val mappedPID = mid.map(identity)
      assert(mappedPID eq mid)
    }
  }

  it should "return correct values when mapped to addition" in {
    def addFive (x: Long) = x + 5
    for ((mid, value, _) <- samplesMessageID) {
      val expected = MessageID(addFive(mid.value))
      val actual = mid.map(addFive)
      assertResult(expected)(actual)
    }
  }

  it should "behave correctly with map2" in {
    val base = MessageID(123)
    for (
      (mid, value, _) <- samplesMessageID;
      op <- operationsMessageID
    ) {
      val expected = MessageID(op(mid.value, base.value))
      val actual   = mid.map2(base)(op)
      assertResult(expected)(actual)
    }
  }

  behavior of "TaskID"

  val samplesTaskID = List(
    (TaskID(0), 0, "τ[0]"),
    (TaskID(4), 4, "τ[4]"),
    (TaskID(6), 6, "τ[6]"),
    (TaskID(10), 10, "τ[10]"),
    (TaskID(1012334), 1012334, "τ[1012334]")
  )

  val operationsTaskID = operationsMessageID

  it should "reply correctly to accessors: name, value" in {
    for ((id, value, name) <- samplesTaskID) {
      assertResult(value)(id.value)
      assertResult(name)(id.name)
    }
  }

  it should "return itself when mapped to identity" in {
    for ((id, _, _) <- samplesTaskID) {
      val mappedPID = id.map(identity)
      assert(mappedPID eq id)
    }
  }

  it should "return correct values when mapped to addition" in {
    def addFive (x: Long) = x + 5
    for ((id, value, _) <- samplesTaskID) {
      val expected = TaskID(addFive(id.value))
      val actual = id.map(addFive)
      assertResult(expected)(actual)
    }
  }

  it should "behave correctly with map2" in {
    val base = TaskID(123)
    for (
      (id, value, _) <- samplesTaskID;
      op <- operationsTaskID
    ) {
      val expected = TaskID(op(id.value, base.value))
      val actual   = id.map2(base)(op)
      assertResult(expected)(actual)
    }
  }

}
