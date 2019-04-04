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

/**
 * =ScalaNeko Framework=
 *
 * ScalaNeko is a framework designed to help with the prototyping of distributed algorithms. It is
 * loosely based on the Neko framework [1] which was programmed in Java more than a decade earlier,
 * mainly by Péter Urbán.
 *
 * Whereas the original Neko framework was designed for performance evaluation and modeling, the
 * main focus of ScalaNeko is to serve as a support for teaching distributed algorithms. Hence, the
 * current version of ScalaNeko only supports simulated execution. However, we still have the
 * intention to support actual distributed execution in a future version, and hence provide a full
 * replacement of the original Neko.
 *
 * ==1. Architecture==
 *
 * In order to effectively use ScalaNeko, it is helpful to understand its general architecture,
 * which can be described as follows:
 *
 * There are several important entities in ScalaNeko:
 *
 *  - The system is what handles the execution engine within the virtual machine and the
 *    initialization procedure. There is exactly one instance running for every virtual machine.
 *    The system also holds a discrete event simulator.
 *    See [[neko.Main]] and [[neko.kernel.NekoSystem]].
 *
 *  - The network simulates the behavior of a network, and is responsible for transmitting messages
 *    between processes. In the current version, it is running over a discrete-event simulation.
 *    See [[neko.network.Network]] and [[neko.kernel.sim.Simulator]].
 *
 *  - The processes are the basic unit of concurrency, and represent a virtual computer connected
 *    through a network. Every process has a unique identity represented by a [[neko.PID]].
 *    A process does nothing by itself and is merely a shell for protocols.
 *    See [[neko.NekoProcess]] and [[neko.ProcessConfig]].
 *
 *  - The protocols are the actual logic of the system and implement the algorithms. A process holds
 *    one or many protocols, which are organized as a stack. There are two kinds of protocols:
 *    active and reactive ones. While active protocols carry their own flow of execution, that is,
 *    act as a thread, concurrently with the system, the reactive protocols only execute code as a
 *    reaction to incoming events.
 *    See [[neko.ActiveProtocol]], [[neko.ReactiveProtocol]], [[neko.Protocol]], and [[neko.ProtocolUtils]].
 *
 *  - Protocols and processes exchange information through events. There are two types of events:
 *    signals and messages. Signals allow protocols '''within the same process''' to notify each
 *    other. In contrast, messages allow protocol instances to communicate
 *    '''across different processes'''. In other words, only messages are transmitted through the
 *    network.
 *    See [[neko.Event]], [[neko.Signal]], [[neko.UnicastMessage]], [[neko.MulticastMessage]],
 *    and [[neko.Wrapper]].
 *
 * A simplified view of the architecture of an execution of ScalaNeko is depicted below:
 * {{{
 *   +-------------------------------------------------------+
 *   |       process p1                    process pn        |
 *   |  +-------------------+         +-------------------+  |
 *   |  | +---------------+ |         | +---------------+ |  |
 *   |  | | protocol p1:A | |         | | protocol pn:A | |  |
 *   |  | +-------------+-+ |         | +-------------+-+ |  |
 *   |  |   |           |   |   ...   |   |           |   |  |
 *   |  | +-+-----------V-+ |         | +-+-----------V-+ |  |
 *   |  | | protocol p1:B | |         | | protocol pn:B | |  |
 *   |  | +-------------+-+ |         | +-------------+-+ |  |
 *   |  +---|-----------|---+         +---|-----------|---+  |
 *   |      |           |                 |           |      |
 *   |  +---+-----------V-----------------+-----------V---+  |
 *   |  |                      network                    |  |
 *   |  +-------------------------------------------------+  |
 *   |                  +------------------+                 |
 *   |                  |     simulator    |                 |
 *   |                  +------------------+       system    |
 *   +-------------------------------------------------------+
 * }}}
 *
 * Creating a ScalaNeko application typically requires to implement the following steps:
 *
 *  a. Implement the protocols. At least, an application will require to implement an active
 *     protocol, but also possibly a number of reusable reactive ones.
 *
 *  a. Each protocol is likely to define its own message types. The most appropriate location for
 *     doing so is in a companion object of the protocol. Messages are best defined as a
 *     `case class` so that they are ensured to be immutable and code for pattern matching is
 *     automatically generated by the compiler.
 *
 *  a. Creating a process initializer that instantiates and connects the protocols of the processes.
 *
 *  a. Creating a main object which provides the basic parameters of the execution, such as the
 *     total number of processes to create and their initializer.
 *
 * The initialization proceeds roughly as illustrated below:
 * {{{
 *        creates            creates
 *   Main ------> NekoSystem ------> Network
 *                           creates
 *                     ''    ------> ProcessInitializer
 *                           creates             creates
 *                     ''    =====>> NekoProcess =====>> Protocol
 * }}}
 *
 * ==2. Creating protocols==
 *
 * A protocol can be either active or reactive. An active protocol is one that executes its own
 * thread, concurrently with that of the other protocols or processes. In contrast, a reactive
 * protocol only executes as a reaction to events, and does not do anything otherwise.
 *
 *
 * ===2.1 Active protocols===
 *
 * An active protocol is typically defined as a subclass of [[neko.ActiveProtocol]].
 *
 * An active protocol has its own thread of control. The code of the protocol is implemented in its
 * method [[neko.ActiveProtocol.run]], which must be defined in the subclass. This code is executed
 * concurrently with the rest of the system.
 *
 * An active protocol has access to operations for sending and receiving message. New messages are
 * sent with the method [[neko.ActiveProtocol.SEND]]. While messages are received through
 * '''blocking''' calls to [[neko.ActiveProtocol.Receive]],
 * as illustrated below. Note that, in order to receive messages of a certain type, the protocol
 * must register by calling [[neko.ActiveProtocol.listenTo]] for this type.
 * {{{
 * class PingPong(c: NekoProcessConfig) extends ActiveProtocol(c, "ping-pong")
 * {
 *   val next = me.map{i => (i+1) % N}
 *   var record = Set.empty[Event]
 *
 *   listenTo(classOf[Ping])
 *   listenTo(classOf[Pong])
 *   def run(): Unit =
 *   {
 *     SEND(Ping(me, next))
 *
 *     Receive {
 *       case Ping(from, _) => SEND(Pong(me, from))
 *       case Pong(from, _) => SEND(Ping(me, from))
 *     }
 *
 *     Receive { m =>
 *       record += m
 *     }
 *   }
 * }
 * }}}
 * It is also possible to override the method [[neko.ActiveProtocol.onReceive]]. By doing so,
 * messages that are matched by `onReceive` are processed reactively upon arrival, while those that
 * are not matched by `onReceive` are stored into the receive queue and must be handled by a
 * blocking call to [[neko.ActiveProtocol.Receive]].
 *
 *
 * ===2.2 Reactive protocols===
 *
 * Most protocols in a process are reactive.
 * A reactive protocol is usually sandwiched between a
 * network and an application (or a lower-level protocol and a higher-level one).
 * The simplest way to implement one is by extending [[neko.ReactiveProtocol]]. The information
 * has two flows: downstream and upstream. This is illustrated in the figure below.
 * {{{
 *             application
 *      |                      ^
 *      V                      |
 *    +----------------------------+
 *    | onSend        DELIVER(...) |
 *    |                            | Reactive protocol
 *    | SEND(...)        onReceive |
 *    +----------------------------+
 *      |                      ^
 *      V                      |
 *              network
 * }}}
 * For the downstream flow (from application to network), the code of the protocol is implemented
 * in the method [[neko.ReactiveProtocol.onSend]], usually implemented as a [[scala.PartialFunction]] which
 * reacts as appropriate to each event. The protocol can itself send messages through the
 * [[neko.ReactiveProtocol.SEND]] method.
 *
 * For the upstream flow (from network to application), the code of the protocol is implemented in
 * the method [[neko.ReactiveProtocol.onReceive]], also implemented as a [[scala.PartialFunction]] which
 * reacts appropriately to each incoming events. Events of a certain type are delivered to the
 * protocol only if it registers to the event type by calling the [[neko.ReactiveProtocol.listenTo]]
 * method on that event type. The protocol can deliver a message to the application through the
 * method [[neko.ReactiveProtocol.DELIVER]].
 *
 * Note that the two flows are not mutually exclusive. It is perfectly valid, and even frequent,
 * for a protocol to call [[neko.ReactiveProtocol.DELIVER]] in [[neko.ReactiveProtocol.onSend]], or to call
 * [[neko.ReactiveProtocol.SEND]] in [[neko.ReactiveProtocol.onReceive]] .
 *
 *
 * ==3. Defining new events (messages and signals)==
 *
 * Let's start with a little bit of terminology. An event denotes anything that happens in the
 * system and is represented by the abstract class [[neko.Event]]. Events can be of two types:
 *
 *  - A signal is an event that occurs within one process, and can go from one protocol to another,
 *    but never cross process boundaries. It is represented by the subclasses of [[neko.Signal]].
 *
 *  - A message is an event that crosses process boundaries, but is typically (but not necessarily)
 *    interpreted by the same protocol in the target process. It is represented by the subclasses
 *    of [[neko.Message]].
 *
 *    A message can be "top-level" or a "wrapper". A top-level message is one that is created by the
 *    sending protocol. It has its own identity, as well as a source and destinations. In contrast,
 *    a wrapper is simply a shell that extends the information of an existing message. It retains
 *    the same identity, source, and destinations, but provides a shell to the message and can add
 *    its own information. This results into messages of three types:
 *
 *    - A [[neko.MulticastMessage]] is a top-level message with multiple destinations. See the
 *      example below on how to define a new message:
 *      {{{
 *   case class Snapshot(
 *       from: PID,
 *       to: Set[PID])
 *     extends MulticastMessage
 *      }}}
 *      NB: The arguments *must* be named <code>from</code> and <code>to</code>.
 *
 *    - A [[neko.UnicastMessage]] is a top-level message with a single destination process.
 *    {{{
 *   case class Token (
 *       from: PID,
 *       to: PID)
 *     extends UnicastMessage
 *    }}}
 *      NB: The arguments *must* be named <code>from</code> and <code>to</code>.
 *
 *    - A [[neko.Wrapper]] is a shell that wraps an existing message. A wrapper can also extend
 *      another wrapper; not only top-level messages. A wrapper preserves the identity, the source
 *      and the destinations of the message it wraps.
 *      {{{
 *        case class SequencedMessage(msg: Message, sn: Int) extends Wrapper(msg)
 *      }}}
 *
 *
 * ==4. Initialization of a process==
 *
 * While processes are created automatically, their protocols are not, and must be initialized and
 * connected. This is done through a process initializer, by providing an instance of
 * [[neko.ProcessInitializer]], whose sole role is to create the protocols of a process and
 * combine them.
 * {{{
 * ProcessInitializer { p =>
 *     val app  = new PingPong(p)
 *     val fifo = new FIFOChannel(p)
 *     app --> fifo
 *   }
 * }}}
 * In the above example, each process is initialized by executing the above code. The code creates
 * two protocols while registering them into the object `p` given as argument (which represents the
 * process being initialized). Then, the two
 * protocols are connected such that all `SEND` operations of protocol `app` are handed to
 * protocol `fifo`. The send operations of protocol `fifo` use the default target which is
 * the network interface of the process.
 *
 * It is also possible to initialize processes differently, by discriminating based on the
 * identifier of the process to initialize. That identifier is obtained from the argument
 * with `p.pid`.
 *
 *
 * ==5. Setting up a new system==
 *
 * A new instance of a ScalaNeko system is created and configured by creating an object that
 * extends [[neko.Main]]. The resulting object becomes a main object and is thus executable
 * ([[neko.Main]] is a subclass of [[scala.App]]).
 *
 * Class [[neko.Main]] requires to set parameters, such as the network topology and
 * the process initializer, as illustrated below:
 * {{{
 *   object PingPongApp extends Main(topology.Clique(3))( ProcessInitializer { p=> ... } )
 * }}}
 *
 * Future planned versions of ScalaNeko will make it possible to define many more parameters, such
 * as the network topologyDescriptor, etc...
 *
 * ==References==
 *
 *  1. Péter Urbán, Xavier Défago, André Schiper:
 *     Neko: A Single Environment to Simulate and Prototype Distributed Algorithms.
 *     J. Inf. Sci. Eng. 18(6): 981-997 (2002).
 *
 * ==Contributors==
 *
 * Lead architect: Xavier Défago
 *
 * Other contributors:
 *  - Naoyuki Onuki (trace system; integration with NekoViewer)
 */
package object neko
{
}
