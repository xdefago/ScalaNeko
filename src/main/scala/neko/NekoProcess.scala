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
 * Date: 21/05/15
 * Time: 15:53
 *
 */
package neko

import com.typesafe.scalalogging.LazyLogging
import neko.config.NekoConfig
import neko.kernel.{ Dispatcher, NekoSystem }

import scala.util.{ Failure, Success }

class NekoProcess(val id: PID, val system: NekoSystem)(config: NekoConfig)
  extends NamedEntity with LazyLogging
{
  def name      = id.name
  def senderOpt = Some(sender)
  
  val network    = system.network
  val dispatcher = Dispatcher.withClassLookup()

  val sender     = new NekoProcess.Sender(this, network)
  val receivers  = IndexedSeq(
    new NekoProcess.Receiver(this, dispatcher)
  )

  val protocols: Set[Protocol] = {

    config.process.initializer match {

      case Success(initializer) =>
        val processConfig  = new NekoProcessConfig(system, this.id, dispatcher, config.tracer)
        initializer.apply(processConfig)
        val protocols = processConfig.registeredProtocols
        processConfig.registeredProtocols

      case Failure(fail) =>
        logger.error(s"Cannot initialize $id: ${fail.getMessage}")
        throw fail
    }
  }

  lazy val neighbors : Set[PID] = system.processSet - id

  def preStart(): Unit  = protocols.foreach { _.preStart() }
  def start(): Unit     = protocols.foreach { _.start() }
  def finish(): Unit    = protocols.foreach { _.onFinish() }
  def shutdown(): Unit  = protocols.foreach { _.onShutdown() }
}


object NekoProcess
{
  type Initializer = Function[NekoProcess, Unit]


  class Sender(p: NekoProcess, network: neko.Sender)
    extends neko.Sender
  {
    def name      = s"${p.name}: network interface"
    def senderOpt = Some(network)
    /**
     * Discard all messages that pass through it.
     * The shutdown code calls this function.
     */
    def dropMessages() = { shouldDropMessages = true }
    private var shouldDropMessages = false

    def send(m: Event) =
      if (! shouldDropMessages) {
        network.send(m)
      }
  }


  class Receiver(p: NekoProcess, dispatcher: Dispatcher)
    extends neko.Receiver
    with LazyLogging
  {
    /**
     * Discard all messages that pass through it.
     * The shutdown code calls this function.
     */
    def dropMessages() = { shouldDropMessages = true }
    private var shouldDropMessages = false

    override def deliver(m: Event) = {
      if (! shouldDropMessages) {
        logger.trace(s"${p.id.name} : deliver(${m.toPrettyString})")
        dispatcher.deliver(m)
      }
    }
  }
}
