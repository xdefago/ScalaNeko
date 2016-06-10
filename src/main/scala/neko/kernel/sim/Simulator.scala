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
 * Time: 19:44
 *
 */
package neko.kernel.sim

import com.typesafe.scalalogging.LazyLogging
import neko.Timer
import neko.kernel.Scheduler

class Simulator(scheduler: Scheduler) extends Timer(scheduler) with Runnable with LazyLogging
{
  def run(): Unit =
  {
    while (scheduler.nonEmpty) {
      scheduler.executeNext()
    }
  }

  def advanceTime(): Unit = {
    val before = now
    scheduler.executeNext()
    logger.trace(s"advanceTime(): $before -> $now")
  }
}
