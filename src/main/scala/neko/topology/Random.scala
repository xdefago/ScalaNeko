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

import scalax.collection.Graph
import scalax.collection.GraphPredef._

/**
 * Factory object to create a random graph (of type Erdös-Renyi).
 *
 * The threshold for deciding whether an edge exists is log(n) / (n-1), where n is the
 * size of the range given as parameter.
 * This choice is because this is a little above the bond percolation threshold for a
 * clique.
 */
object Random extends TopologyFactory
{
  private var seeds = Map.empty[Range, Long]

  /**
   * Generates a random graph using Erdös-Renyi's method and using log(n) / (n-1) as the
   * probability of existence of an edge over a clique.
   *
   * @param range the range of process numbers on which to generate the random graph.
   * @return
   */
  def apply(range: Range) : Topology =
  {
    if (! seeds.isDefinedAt(range)) seeds += ( range -> { util.Random.nextLong() } )
    val seed   = seeds(range)
    val random = new util.Random(seed)

    val N = range.size
    val epsilon : Double = 2 * math.log(N) / math.sqrt(N)

    val threshold = if (N>2) ((1+epsilon) * math.log(N)) / (N-1) else 1

    Topology.from (
      Graph.from(
        nodes = range,
        edges =
          for {
            i <- range
            j <- range
            if j > i && random.nextDouble <= threshold
          } yield i ~ j
      )
    )
  }

  def apply(n: Int) : Topology = this.apply(0 until n)
}
