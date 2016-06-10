/**
 *
 * Copyright 2014 Xavier Defago
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
 * Date: 09/06/2014
 * Time: 16:29
 *
 */
package neko.util

import neko._

trait MutexClient
{ this: Receiving with Listener =>

  def request() { SEND(MutexClient.Request) }
  def release() { SEND(MutexClient.Release) }
  listenTo(MutexClient.CanEnter.getClass)
}

object MutexClient
{
  case object Request  extends Signal
  case object CanEnter extends Signal
  case object Release  extends Signal
}
