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
 * Date: 23/05/15
 * Time: 23:46
 *
 */
package neko.kernel

import neko.{Event, Receiver}

trait Dispatcher extends Receiver
{
  def orphanEventHandler: Receiver

  def registerFor(clazz: Class[_<:Event], proto: Receiver): Unit
  def unregisterFor(clazz: Class[_<:Event], proto: Receiver): Unit
  def unregisterAllFor(clazz: Class[_<:Event]): Unit
  def protocolsFor(clazz: Class[_<:Event]) : Seq[Receiver]

  def deliver(ev: Event) =
  {
    val protocols = this.protocolsFor(ev.getClass)
    for (proto <- protocols) {
      proto.deliver(ev)
    }
  }
}

object Dispatcher
{
  def apply(): Dispatcher = this.withClassLookup()

  def withClassLookup(): Dispatcher = new SimpleDispatcher()
  //def withMatchAll(): Dispatcher = ???
}


class SimpleDispatcher extends Dispatcher
{
  // TODO: add "hook" categories: 'matchAll', and 'unmatchedEvents'

  val orphanEventHandler: Receiver = new Receiver {
      def deliver (m: Event) =
        Console.err.println("WARNING: orphan message (did you forget listenTo?): "+m + "("+m.getClass.getName+")")
    }

  private val handleOrphanEvents = Seq(orphanEventHandler)

  var protocolsForMessage =
    Map
      .empty[Class[_<:Event], List[Receiver]]
      .withDefaultValue(Nil)

  def registerFor(clazz: Class[_<:Event], proto: Receiver): Unit =
    synchronized {
      val protocols = protocolsForMessage(clazz)
      if (! protocols.contains(proto)) {
        protocolsForMessage = protocolsForMessage.updated(clazz, proto :: protocols)
      }
    }

  def unregisterFor(clazz: Class[_<:Event], proto: Receiver): Unit =
    synchronized {
      val protocols = protocolsForMessage(clazz)
      if (protocols.contains(proto)) {
        val excluding = protocols.filterNot(_ eq proto)
        protocolsForMessage = protocolsForMessage.updated(clazz, excluding)
      }
    }

  def unregisterAllFor(clazz: Class[_<:Event]): Unit =
    synchronized {
      protocolsForMessage = protocolsForMessage.updated(clazz, Nil)
    }

  def protocolsFor(clazz: Class[_<:Event]) : Seq[Receiver] =
    synchronized {
      val protocols = protocolsForMessage(clazz)
      if (protocols.nonEmpty)
        protocols
      else
        handleOrphanEvents
    }
}
