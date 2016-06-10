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
 * Date: 10/06/15
 * Time: 12:59
 *
 */
package neko.util

import neko.PID

class Channel(_pi: PID, _pj: PID) extends Ordered[Channel]
{
  val (pi, pj) = if (_pi < _pj) (_pi, _pj) else (_pj, _pi)

  def compare (o: Channel): Int = {
    if (pi == o.pi) {
      pj compare o.pj
    } else {
      pi compare o.pi
    }
  }

  override def toString: String = s"Channel($pi,$pj)"

  def toSimpleString: String = s"<${pi.name},${pj.name}>"
}

object Channel
{
  def apply(_pi: PID, _pj: PID): Channel = new Channel(_pi, _pj)
  def unapply(c: Channel): Option[(PID, PID)] = Some(c.pi, c.pj)
}
