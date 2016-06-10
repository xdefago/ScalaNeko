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
 * Date: 01/06/15
 * Time: 19:04
 *
 */
package neko.network.sim

import neko.kernel.NekoSystem
import neko.kernel.sim.Simulator
import neko.network.AbstractNetwork

import scala.util.Try


abstract class SimNetwork(system: NekoSystem, val simulator: Simulator) extends AbstractNetwork(system)
{
  assume(system != null)
  assume(simulator != null)

}


object SimNetwork
{
  /**
   * creates a new instance of [[SimNetwork]] from the given class name.
   * @param className name of the subclass of [[SimNetwork]] to instantiate.
   * @return `Success` with the new instance if successful, or `Failure` otherwise.
   */
  def forName(className: String): Try[Class[_<:SimNetwork]] =
    Try {
      Class
        .forName(className)
        .asSubclass(classOf[SimNetwork])
    }
}
