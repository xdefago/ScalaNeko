/*
 * Copyright 2017 Xavier Défago (Tokyo Institute of Technology)
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
package neko.util

import neko._


trait SnapshotClient
{ this: Receiving with Listener =>

  def initiate() { SEND(SnapshotClient.Initiate) }

  listenTo(SnapshotClient.SaveState.getClass)
  listenTo(classOf[SnapshotClient.SnapshotDone])
}


object SnapshotClient
{
  case object Initiate extends Signal
  case object SaveState extends Signal
  case class SnapshotDone(recorded: Set[Message]) extends Signal
}
