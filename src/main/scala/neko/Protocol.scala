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
 * Time: 16:07
 *
 */
package neko


import neko.kernel.NekoSystem

/**
 * Defines the basic operations that a protocol must provide.
 *
 * An application programmer will not use this trait directly, but rather use its two main
 * subclasses: [[ActiveProtocol]] for active protocols, and [[ReactiveProtocol]] for reactive protocols.
 */
trait Protocol extends NamedEntity
{
  /**
   * returns the system, allowing to access global information.
   * @return a reference to the system
   */
  def system: NekoSystem

  /**
   * returns the identifier of the process on which this instance of the protocol is ruuning.
   * @return identifier of the process
   */
  def process: PID

  /**
   * returns the identifier of the protocol
   * @return identifier of the protocol
   */
  def id : ProtoID

  /**
   * returns the name of the protocol
   * @return name of the protocol
   */
  def name = s"${process.name}:${id.name}"
  
  override def simpleName = id.name
  override def context    = Some(process)

  /**
   * override this method to perform initializations just before the process begins to run.
   */
  def preStart(): Unit = {}

  /**
   * override this method to perform initialization just as the process starts.
   */
  def start(): Unit    = {}

  /**
   * override this method to perform take downs just after this process has finished.
   */
  def onFinish(): Unit = {}

  /**
   * override this method to perform take downs after all processes have finished and before the
   * process shuts down.
   */
  def onShutdown(): Unit = {}
}



