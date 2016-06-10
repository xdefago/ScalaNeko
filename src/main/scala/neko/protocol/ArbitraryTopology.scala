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
 * Date: 25/06/2014
 * Time: 16:07
 *
 */
package neko.protocol

import com.typesafe.scalalogging.LazyLogging
import neko._
import neko.protocol.ArbitraryTopology._
import neko.util.Topology

import scalax.collection.Graph
import scalax.collection.GraphEdge.UnDiEdge

/**
 * Support for simulating arbitrary network topologies.
 *
 * The topologyDescriptor is given as a graph
 * (see [[http://www.scala-graph.org/api/core/api/#scalax.collection.Graph]]).
 * A message sent by a process ''p''
 * is delivered to another process ''q'' only if ''q'' is a neighbor of ''p'' in the graph.
 * The protocol can also be queried through the [[neko.util.Topology]] trait, to obtain the set of neighbors
 *
 * By default, the topologyDescriptor is a clique of all the processes, but other ready-made graphs can also
 * be found in [[neko.util.Topology]].
 *
 * @param config         the process on which the protocol runs.
 * @param topology  a graph describing the whole network topologyDescriptor. A warning is issued when the graph is not connected.
 */
class ArbitraryTopology(config: ProcessConfig, val topology: Graph[Int,UnDiEdge])
  extends ReactiveProtocol(config, "arbitrary topologyDescriptor")
    with Topology with LazyLogging
{
  if (! topology.isConnected)
    logger.warn(s"WARNING: ${this.getClass.getSimpleName}: Topology is not connected in\n$topology")

  protected[this] val myNode = topology.get(me.value)

  val neighborhood : Set[Int] = myNode.neighbors.map { _.toInt }

  def onSend = {
    case ie : Signal => SEND(ie)

    case m  : Message =>
      SEND(MessageShell(me, m.destinations intersect neighborhood.map(PID), m))
      if (m.destinations.contains(me)) DELIVER(m)
  }

  listenTo(classOf[MessageShell])
  def onReceive = {
    case MessageShell(_,_,m,_) => DELIVER (m)
  }
}


object ArbitraryTopology
{
  case class MessageShell(from: PID, to: Set[PID], content: Message, id: MessageID = MessageID.auto())
    extends MulticastMessage(from, to, id)
}
