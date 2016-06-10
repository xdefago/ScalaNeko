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
 * Date: 09/07/15
 * Time: 17:39
 *
 */
package neko.topology

object Ladder extends TopologyFactory
{
  def apply(n: Int) : Topology = {
    assume(n>2)
    (for (i <- 2 until n) yield Clique(i-2 to i)).reduce(_ union _)
  }
}
