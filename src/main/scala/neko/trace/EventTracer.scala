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
 * Date: 31/05/15
 * Time: 13:55
 *
 */
package neko.trace

import neko._
import neko.util.Time


trait EventTracer
{
  def send   (at: Time, by: PID)(event: Message)
  def deliver(at: Time, by: PID)(event: Message)
  def signal (at: Time, by: PID)(sig: Signal)
}

object EventTracer
{
}

trait EventFormatter
{
  def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event): String
}

object EventFormatter
{
  sealed trait EventKind { def name: String }
  case object Send extends EventKind { val name = "SND" }
  case object Recv extends EventKind { val name = "RCV"}
  case object Sig  extends EventKind { val name = "SIG"}

  object SimpleEventFormatter extends EventFormatter
  {
    protected def formatTime(time: Time): String = Time.formatTimeSeconds(time)
    protected def formatKind(kind: EventFormatter.EventKind): String = kind.name
    protected def formatClass(clazz: Class[_]): String = clazz.getSimpleName
    protected def formatEvent(ev: Event): String = ev.toString

    def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event): String =
    {
      val sKind = formatKind(kind)
      val sTime = formatTime(time)
      val sEvent = formatEvent(ev)
      s"[${by.name}] $sTime $sKind $sEvent"
    }
  }
}

object ConsoleEventTracer extends EventTracer
{
  import EventFormatter.SimpleEventFormatter.stringFor
  import EventFormatter._

  def send   (at: Time, by: PID)(m: Message)  = println(stringFor(Send, at, by, m))
  def deliver(at: Time, by: PID)(m: Message)  = println(stringFor(Recv, at, by, m))
  def signal (at: Time, by: PID)(sig: Signal) = println(stringFor(Sig, at, by, sig))
}


object NullEventTracer extends EventTracer
{
  def send   (at: Time, by: PID)(m: Message)  = {}
  def deliver(at: Time, by: PID)(m: Message)  = {}
  def signal (at: Time, by: PID)(sig: Signal) = {}
}
