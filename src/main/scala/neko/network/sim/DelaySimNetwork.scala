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
package neko.network.sim

import com.typesafe.scalalogging.LazyLogging
import neko._
import neko.kernel.NekoSystem
import neko.kernel.sim.Simulator
import neko.util.Time

/**
 * generic simulated network that adds a delay to messages.
 *
 * @param system  system in which the network runs.
 * @param sim     simulator to be used by the network.
 */
abstract class DelaySimNetwork(system: NekoSystem, sim: Simulator)
  extends SimNetwork(system, sim) with LazyLogging
{
  val neighborhoodFor: Map[PID, Set[PID]] =
    system.processSet.map { p => p -> (topology.neighborsFor(p).getOrElse(Set.empty) + p) }.toMap

  /**
   * returns the amount of time that message `m` must be delayed before being delivered to process
   * `dest`. A concrete instance must implement this method.
   *
   * @param dest  intended destination of the message
   * @param m     message to be delivered
   * @return      delivery delay for that message, or `None` if the message is to be dropped.
   */
  def delayForMessage(dest: PID, m: Message): Option[Time]

  override def sendTo(dest: PID, m: Message): Unit =
  {
    if (neighborhoodFor.get(m.from).forall( _.contains(dest) )) {
      val optDelay = delayForMessage(dest, m)
      logger.trace(s"sendTo(${dest.name}, ${m.toPrettyString}); w/delay:${optDelay.map(_.asNanoseconds)}")
      optDelay.foreach { delay =>
        simulator.scheduleAfter(delay) {
          now => super.sendTo(dest, m)
        }
      }
    }
  }
}
