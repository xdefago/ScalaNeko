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
 * Factory object to create a geometric random graph.
 *
 * The idea is to generate positions for the nodes uniformly at random in a 1 x 1 square plane.
 * Then, given a radius r, two nodes are neighbors iff. their Euclidean distance is less than r.
 *
 * The critical radius is calculated from the following formula (linked to percolation theory):
 *
 * {{{
 *   val r = math.sqrt( (math.log(N) + alpha) / (math.Pi * N))
 * }}}
 * where alpha is an adjustment parameter to increase the likelihood that the resulting graph is
 * connected.
 */
object GeometricRandom extends TopologyFactory
{
  private var seeds = Map.empty[Range, Long]

  /**
   * Generates a geometric random graph, in which nodes are randomly positioned on the plane, and
   * an edge exists between two nodes only if their distance is less than some chosen constant.
   *
   * @param range   the range of process numbers on which to generate the random graph.
   * @param alpha   parameter to increase the likelihood that the resulting graph is connected.
   * @return
   */
  def apply(range: Range, alpha : Double) : Topology =
  {
    case class Position(x:Double, y:Double, rank:Int)

    if (!seeds.isDefinedAt(range)) seeds += (range -> { util.Random.nextLong() })
    val seed = seeds(range)
    val random = new util.Random(seed)

    val N = range.size

    val epsilon : Double = 2 + math.log(N) / math.sqrt(N)

    // compute r near the critical radius for connectivity [see PhD dissertation of Chen Avin; Chapter 4]
    // http://www.bgu.ac.il/~avin/papers/diss.pdf
    // CHOICES:
    // - uses alpha = 1
    // - uses \gamma_n = log(n)
    // - adds correction for using unit square instead of unit disc
    // - adds epsilon factor to adjust for small values of N

    val r_square = (2 * math.log(N)) / (math.Pi * N)

    // val r_square = (math.log(N) + alpha) / (math.Pi * N)
    val r = math.sqrt( r_square ) * epsilon

    val positions =
      for {
        i <- range
        x = random.nextDouble()
        y = random.nextDouble()
      } yield new Position(x,y,i)

    val edges =
      for {
        loc1 <- positions
        loc2 <- positions
        if (loc1.rank < loc2.rank) &&
           math.hypot(loc1.x - loc2.x, loc1.y - loc2.y) < r
      } yield loc1.rank ~ loc2.rank

    Topology.from ( Graph.from(nodes = range, edges = edges) )
  }

  def apply(range: Range) : Topology          = this.apply(range, 1.0)
  def apply(n: Int, alpha: Double) : Topology = this.apply(0 until n, alpha)
  def apply(n: Int) : Topology                = this.apply(0 until n)
}
