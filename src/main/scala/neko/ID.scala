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
package neko

import java.util.UUID

/**
 * Generic type to represent all kinds of identifiers in ScalaNeko. The type parameter refers to
 * the type of value of the identifiers and depends on each kind of identifiers.
 *
 * The main subclasses relevant to a protocol programmer are:
 *  - [[PID]] for processes
 *  - [[MessageID]] for messages
 *  - [[TaskID]] for scheduled tasks (see [[Timer]]
 *
 * @tparam T type of the value encapsulated in the identifier
 */
trait ID[T]
{
  /**
   * Same type as the identifier to be set to be the same type as the subclass of identifier.
   * This is necessary for the generic functions [[map]] and [[map2]].
   */
  type SameType <: ID[T]

  /**
   * Value encapsulated by the identifier.
   *
   * @return value of the identifier
   */
  def value : T

  /**
   * Output-friendly string representation of the identifier.
   *
   * @return string representation
   */
  def name : String

  /**
   * Applies a function to the value of the identifier and returns a new identifier with the result.
   *
   * @param f function to apply on the value
   * @return  identifier with the result of applying `f()`
   */
  def map(f: (T)=>T): SameType                       = idWith(f(value))

  /**
   * Applies a function combining the values of two identifiers and returns a new identifier with
   * the result.
   *
   * @param that  other identifier to combine
   * @param f     function to apply on the values of the two identifiers
   * @tparam B    type of value of the other identifier
   * @return      identifier with the result of applying `f()`
   */
  def map2[B<:T](that: ID[B])(f: (T,B)=>T): SameType = idWith(f(this.value,that.value))

  /**
   * Returns a new instance of the same type of identifier initialized with the value given as
   * parameter. Required by [[ID!.map]] and [[ID!.map2]].
   *
   * @param newID  value of the new instance of identifier
   * @return       a new identifier
   */
  protected def idWith(newID: T): SameType
}


/**
 * Class to represent process identifiers. The field [[value]] corresponds to the index of the
 * process and is guaranteed to be unique. When the execution is simulated on a single machine, the
 * PIDs are consecutive numbers from `PID(0)` to  `PID(N-1)`.
 *
 * In addition, there is a total order on process identifiers, which allows to compare them.
 *
 * @param value the index of the process
 */
case class PID(value: Int) extends ID[Int] with Ordered[PID]
{
  type SameType = PID

  /**
   * compares with another PID. The result is the same as comparing their respective index.
   * @param that the other PID with which to compare
   * @return     zero if equal, negative if this < that and positive otherwise.
   */
  def compare (that: PID): Int = this.value.compare(that.value)

  def name = s"p$value"

  protected def idWith(newID: Int) = if (value == newID) this else copy(value = newID)
}


/**
 * Class to represent protocol identifiers. The field [[value]] corresponds to the
 * nickname of the protocol.
 *
 * @param value the nickname of the protocol
 */
case class ProtoID(value: String) extends ID[String]
{
  type SameType = ProtoID

  def name = s"πρ[$value]"
  protected def idWith(newID: String): ProtoID = if (value == newID) this else copy(value = newID)
}


/**
 * Class to represent unique identifiers for instances of [[Signal]]. The field [[SignalID!.value]]
 * corresponds to a universal unique ID ([[java.util.UUID]]) which is guaranteed to be unique with
 * very high probability for any two different instances of [[Signal]]. This identifier will seldom
 * be used by application programmers.
 * @param value the unique ID of the signal
 */
case class SignalID(value: UUID) extends ID[UUID]
{
  type SameType = SignalID

  def name = s"σ$value"
  protected def idWith(newID: UUID) = if (value == newID) this else copy(value = newID)
}


/**
 * Class to represent the identifier of a message. Although it is unlikely (though not forbidden)
 * that an application programmer will rely directly on message identifiers, it is not entirely
 * transparent since they must be generated properly when creating new messages.
 *
 * To make matters simple and to avoid obscuring the code of protocols, it is '''very strongly'''
 * recommended to enforce auto-incrementation when declaring new messages. This is done by having
 * the `id` field declared last and assigned with a default value obtained through the class method
 * [[MessageID#autoIncrement]], as illustrated below:
 * {{{
 *   case class Token(
 *       from: PID,
 *       to: PID,
 *       id: MessageID = MessageID.autoIncrement()
 *     ) extends UnicastMessage(from, to, id)
 * }}}
 *
 * @param value the raw value of the ID
 */
case class MessageID(value: Long) extends ID[Long] with Ordered[MessageID]
{
  type SameType = MessageID

  def compare (that: MessageID): Int = this.value.compare(that.value)

  def name = s"m$value"
  protected def idWith(newID: Long): MessageID = if (value == newID) this else copy(value = newID)
}

object MessageID
{
  private var lastID: Long = 0

  /**
   * generates and returns a new, auto-incremented, message identifier.
   *
   *To make matters simple and to avoid obscuring the code of protocols, it is '''very strongly'''
   * recommended to enforce auto-incrementation when declaring new messages. This is done by having
   * the `id` field declared last and assigned with a default value obtained through this method,
   * as illustrated below:
   * {{{
   *   case class Token(
   *       from: PID,
   *       to: PID,
   *       id: MessageID = MessageID.auto()
   *     ) extends UnicastMessage(from, to, id)
   * }}}
   *
   * @return the new unique message identifier
   */
  def auto() : MessageID = synchronized {
    lastID += 1
    MessageID(lastID)
  }

  /**
   * generates and returns a new, auto-incremented, message identifier.
   *
   * Deprecated: replace with the shorter [[auto]]
   *
   * @return the new unique message identifier
   */
  @deprecated(message="replace with the shorter MessageID.auto()", since="0.11")
  def autoIncrement() = auto()
}
