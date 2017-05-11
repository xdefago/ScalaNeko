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
 * Basic trait to define all processable events in ScalaNeko.
 * There are two basic kinds of events:
 *
 * - Events that are internal to a given process and used as notifications across protocol layers
 *   are called "signals". Such events must inherit from the class [[Signal]], and are always local
 *   to the process on which they were created.
 *
 * - Events that eventually transit through a network are called "messages". Such events must
 *   inherit from one of the subclasses of [[Message]], namely:
 *   - [[UnicastMessage]] are for messages with a single destination process, that are generated by
 *     a protocol.
 *   - [[MulticastMessage]] are for messages with multiple destinations, that are generated by a
 *     protocol.
 *   - [[Wrapper]] are for wrapping existing messages with added information (e.g., a sequence
 *     number), as is typically done for payload messages. Even without adding new information, a
 *     typical use case is to ensure that the message will first be processed by the same protocol
 *     (at the destination process) before its content is delivered to the higher layer.
 *
 *
 *
 * Note that all events must be immutable. It is an error to create mutable messages and, even
 * though this is not necessarily detected by the compiler, mutable messages will almost surely
 * lead to faulty behavior that is difficult to reproduce and thus will be extremely difficult to
 * detect.
 */
sealed trait Event extends Immutable
{
  type IDType <: ID[_]
  def id: IDType
  def toPrettyString: String
}

/**
 * Basic class for internal events.
 *
 * All messages used for signaling within a process should inherit from this class.
 * Just like other messages, a protocol must listen to it for an internal event to be delivered
 * successfully. Unlike messages, no error is raised in case there is no protocol to
 * listen to it.
 *
 * Example:
 * {{{
 *   case object Terminate extends Signal
 *   case class ChangeID(id: Int) extends Signal
 * }}}
 */
abstract class Signal extends Event
{
  type IDType = SignalID
  lazy val id = SignalID(UUID.randomUUID())
  def toPrettyString: String = this.toString
}

/**
 * Basic trait to define all messages. A message is an event that is supposed to propagate through
 * the network, and thus cross process boundaries.
 *
 * This superclass defines the basic information that every message must hold. It is not possible
 * to extend this class directly, but rather messages are defined by extending one of its three
 * subclasses: [[UnicastMessage]], [[MulticastMessage]], or [[Wrapper]].
 *
 * The choice of a parent class (among the three classes described above) depends on the nature of
 * the message to define.
 *
 *  - [[UnicastMessage]] is for a message that is generated by the protocol (typically a control
 *    message) and it makes no sense for that message to have multiple destinations (e.g., a token
 *    that circulates on a logical ring).
 *
 *  - [[MulticastMessage]] is for a message that is generated by the protocol and may have multiple
 *    destinations.
 *
 *  - [[Wrapper]] is for adding information to an existing message, typically obtained from the
 *    application (or a higher-level protocol). This occurs for instance, when our protocol needs
 *    to add sequence numbers or other similar information to an existing payload message.
 *
 * NB: Note that, just like other events, all instances of [[Message]] and their subclass must be
 * immutable. It is an error to define a mutable subclass or else behavior is undetermined.
 */
sealed trait Message extends Event
{
  self =>
  type IDType = MessageID

  /**
   * The identifier of the message.
   * @return the identifier of the message
   */
  def id   : MessageID

  /**
   * The identifier of the sending process. Typically initialized with `me`.
   * @return the identifier of the sending process
   */
  def from : PID

  /**
   * The set of destination processes (well, their ID anyways).
   * @return the set of destination processes.
   */
  def destinations : Set[PID]

  lazy val toPrettyString: String =
  {
    val className = self.getClass.getSimpleName
    val from = self.from.name
    val to = destinations.map (_.name).mkString ("{", ",", "}")

    val argNames =
      self.getClass
        .getDeclaredFields
        .map(_.getName)
        .filterNot(n=>n=="from" || n=="to" || n=="id")

    val argString =
      argNames
        .map(name => /* name + "=" + */ self.getClass.getDeclaredMethod(name).invoke(self).toString)
        .mkString(",")
    className + "(" + from + "," + to + "," + argString + ")"
  }
}

/**
 * Base class to define a new message with one single destination process (unicast).
 *
 * Each new message must be a subclass of either [[UnicastMessage]] or of [[MulticastMessage]].
 * For a protocols to add information to an existing message, it is necessary to define instead
 * a wrapper message; i.e., a subclass of [[Wrapper]].
 *
 * Every subclass of [[UnicastMessage]] or of [[MulticastMessage]] must necessarily define a
 * message identifier, preferrably using the method described in the examples.
 *
 * Typical declarations of top-level messages:
 * {{{
 *   case class Token (
 *       from: PID,
 *       to: PID,
 *       id: MessageID = MessageID.autoIncrement())
 *     extends UnicastMessage(from,to,id)
 *
 *   case class Ack (
 *       from: PID,
 *       to: PID,
 *       id: MessageID = MessageID.autoIncrement())
 *     extends UnicastMessage(from,to,id)
 *
 *   case class FIFO (
 *       from: PID,
 *       to: PID,
 *       sn: Long,
 *       payload: Message,
 *       id: MessageID = MessageID.autoIncrement())
 *     extends UnicastMessage(from,to,id)
 * }}}
 *
 * Instantiation of top-level messages:
 * {{{
 *   import neko._
 *
 *   val next = me.map { i => (i+1) % N }
 *   val m1 = Token(me, next)
 *   val m2 = Ack(me, next)
 *   val m3 = FIFO(me, next, seqnum, msg)
 * }}}
 * Thanks to being auto-incremented, it is not necessary to provide an id when creating a new
 * message. However, for this to work properly, it is essential that new messages are declared are
 * indicated above.
 *
 * Defining actual messages as a `case` class is a recommended practice and highly convenient since
 * it allows for pattern matching without requiring any additional work.
 * {{{
 *   def onReceive = {
 *     case Token(from,_,_) if from < me =>
 *         // e.g., getting a token from a process with lower id
 *
 *     case FIFO(_,_,sn,m,_) if deliverNext == sn =>
 *        deliverNext += 1
 *        DELIVER(m)
 *   }
 * }}}
 * Note that the pattern matching ends with an additional wildcard. While it was not necessary to
 * provide a message ID as it is generated by default, that argument exists and must thus appear in
 * the pattern (even if only as a wildcard). For instance, while {{{Token}}} was instantiated with
 * only two arguments, it really has three, as one can see in the pattern matching example.
 */
abstract class UnicastMessage(from: PID, to: PID, id: MessageID) extends Message
{
  def destinations = Set(to)
}


/**
 * Base class to define a new message with multiple destination processes (multicast).
 *
 * Each new message must be a subclass of either [[UnicastMessage]] or of [[MulticastMessage]].
 * For a protocols to add information to an existing message, it is necessary to define instead
 * a wrapper message; i.e., a subclass of [[Wrapper]].
 *
 * Every subclass of [[UnicastMessage]] or of [[MulticastMessage]] must necessarily define a
 * message identifier, preferably using the method described in the examples.
 *
 * Typical declarations of top-level messages:
 * {{{
 *   case class Snapshot(
 *       from: PID,
 *       to: Set[PID],
 *       id: MessageID = MessageID.autoIncrement())
 *     extends MulticastMessage(from,to,id)
 *
 *   case class ViewChange(
 *       from: PID,
 *       to: Set[PID],
 *       viewNum: Long,
 *       epochNum: Long,
 *       id: MessageID = MessageID.autoIncrement())
 *     extends MulticastMessage(from,to,id)
 *
 *   case class Heartbeat(
 *       from: PID,
 *       to: Set[PID],
 *       sentAt: Time,
 *       sn: Long, id: MessageID = MessageID.autoIncrement())
 *     extends MulticastMessage(from,to,id)
 * }}}
 *
 * Instantiation of top-level messages:
 * {{{
 *   import neko._
 *
 *   val m1 = Snapshot(me, neighbors)
 *   val m2 = ViewChange(me, ALL, myView, myEpoch)
 *   val m3 = Heartbeat(me, neighbors, now, seqnum)
 * }}}
 * Thanks to being auto-incremented, it is not necessary to provide an id when creating a new
 * message. However, for this to work properly, it is essential that new messages are declared are
 * indicated above.
 *
 * Defining actual messages as a `case` class is a recommended practice and highly convenient since
 * it allows for pattern matching without requiring any additional work.
 * {{{
 *   def onReceive = {
 *     case Snapshot(from,_,_) if from < me =>
 *         // e.g., getting a snapshot from a process with lower id
 *
 *     case ViewChange(_,_,view,epoch,_) if view > currentView =>
 *        currentView = view
 *        // ...
 *   }
 * }}}
 * Note that the pattern matching ends with an additional wildcard. While it was not necessary to
 * provide a message ID as it is generated by default, but that argument exists and must thus appear
 * in the pattern (even if only as a wildcard). For instance, while {{{Snapshot}}} was instantiated
 * with only two arguments, it really has three, as one can see in the pattern matching example.
 */
abstract class MulticastMessage(from: PID, to: Set[PID], id: MessageID) extends Message
{
  def destinations = to
}


/**
 * Basic class from messages used to add information to existing messages (wrappers).
 *
 * The resulting message retains all information from the original, and can add some specific
 * to the protocol.
 *
 * Typical message definition:
 * {{{
 *   case class SequencedMessage(msg: Message, sn: Int) extends Wrapper(msg)
 * }}}
 *
 * typical use:
 * {{{
 *   var sequenceNumber = 0
 *   def onSend = {
 *     case m => SEND(SequencedMessage(m, sequenceNumber)) ; sequenceNumber += 1
 *   }
 * }}}
 * @param m message to be wrapped
 */
abstract class Wrapper(m: Message) extends Message
{
  def destinations   = m.destinations
  def from = m.from
  def id   = m.id
}

//object Message
