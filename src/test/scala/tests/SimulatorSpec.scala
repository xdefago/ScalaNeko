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
 * Date: 02/06/15
 * Time: 10:31
 *
 */
package tests

import neko.kernel.sim.Simulator
import neko.kernel.{ParallelScheduler, SequentialScheduler}
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext

class SimulatorSpec extends FlatSpec with SimulatorBehaviors
{
  behavior of "Simulator (w/sequential scheduler)"

  it should behave like discreteEventSimulator(new Simulator(new SequentialScheduler()))

  behavior of "Simulator (w/parallel scheduler)"

  it should behave like discreteEventSimulator(new Simulator(new ParallelScheduler(ExecutionContext.global)))
}
