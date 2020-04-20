/*
 * Copyright 2019 Xavier Défago (Tokyo Institute of Technology)
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

package neko


/**
 * identifier of a task obtained from scheduling an action through an instance of [[Timer]].
 * Tasks identifiers are totally ordered, which allows to compare them.
 *
 * @param value identifying number of the task.
 */
case class TaskID(value: Long) extends ID[Long] with Ordered[TaskID]
{
  type SameType = TaskID

  def name = s"τ[$value]"
  protected def idWith(newID: Long): TaskID = if (value == newID) this else copy(value = newID)

  def compare (that: TaskID): Int = this.value compare that.value
}


object TaskID
{
  private var lastID: Long = 0
  protected[neko] def autoIncrement() : TaskID = {
    lastID += 1
    TaskID(lastID)
  }
}
