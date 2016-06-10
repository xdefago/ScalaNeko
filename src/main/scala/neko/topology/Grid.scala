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
 * Date: 27/06/15
 * Time: 17:32
 *
 */
package neko.topology

/**
 * Factory object to generate a grid topologyDescriptor.
 */
object Grid extends TopologyFactory
{
  /**
   * Generates a grid topologyDescriptor, with process 0 at one corner.
   *
   * The grid generated is a possibly incomplete square.
   *
   * @param range the range of process numbers on which to generate the grid
   * @return
   */
  def apply(range: Range) : Topology =
  {
    val N    = range.size
    val side = math.ceil(math.sqrt(N)).toInt

    Topology.from ( Topology.gridOf (side, side, range.min) )
  }

  def apply(n: Int) : Topology = this.apply(0 until n)
}
