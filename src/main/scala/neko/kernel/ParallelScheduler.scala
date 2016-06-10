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
 * Time: 19:48
 *
 */
package neko.kernel

import java.util.concurrent.CountDownLatch

import neko.util.Time

import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}

class ParallelScheduler(executor: ExecutionContext) extends Scheduler
{
  logger.warn("Beware that ParallelScheduler is not inherently threadsafe! Use at your own risks")

  implicit val exec = executor

  protected[this] def executeBatch(time: Time, tasks: SortedSet[Task]): Unit =
  {
    // FIXME: NOT THREADSAFE !!!!
    // This will fail if two events related to the same process are present in the batch.
    // Need info about process associated to task, but that would make it difficult to use the
    // scheduler inside a network.

    // FIXME: use fork-join pattern instead of CountdownLatch

    val doneSignal = new CountDownLatch(tasks.size)

    // execute tasks in batch
    tasks foreach (executeTask(time, _, doneSignal))

    // wait for all of them to complete
    doneSignal.await()
  }

  protected def executeTask (time: Time, task: Task, done: CountDownLatch): Unit =
  {
    // Simple scheduler which executes tasks sequentially (no concurrency)
    Future {
      task.executeAt(time)
      done.countDown()
    }
  }
}
