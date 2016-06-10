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
 * Date: 23/05/15
 * Time: 20:44
 *
 */
package neko.network.sim

import neko._
import neko.kernel.NekoSystem
import neko.kernel.sim.Simulator
import neko.util.Time

import scala.util.Random


/**
 * simulated network that adds a random delay to messages.
 *
 * @param system  system in which the network runs.
 * @param sim     simulator to be used by the network.
 */
class RandomSimNetwork(system: NekoSystem, sim: Simulator) extends DelaySimNetwork(system, sim)
{
  protected val lambda = system.config.network.lambda
  protected val rand   = new Random(RandomSimNetwork.randomSeed.nextLong())

  def delayForMessage(dest: PID, m: Message): Option[Time] =
    Some(Time.second * (-lambda * math.log(rand.nextDouble())))
}

object RandomSimNetwork
{
  protected val randomSeed = new Random(0xcafebabeL)
}
