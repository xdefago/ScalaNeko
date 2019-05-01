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
package neko.kernel

import java.util.concurrent.BrokenBarrierException

import neko.exceptions.ActivityAbortedError
import neko.{ ActiveProtocol, Protocol, Timer }

/**
 * Trait that should be mixed in with any protocol having its own thread of execution (i.e., an
 * instance of [[java.lang.Runnable]]). It is unlikely that an application programmer will ever
 * have to use this trait directly, but will instead extend [[ActiveProtocol]] for the top-level
 * active part of the protocol stack, or rely on [[Timer]] with a reactive protocol to generate
 * events spontaneously.
 */
trait ManagedActivity
{ self: Runnable with Protocol =>

  def name: String // inherited from Protocol

  protected[this] val activityManager = system.activityManager
  protected[this] val activityID      = activityManager.registerActivity(this)
  protected[this] def willWait(): Unit   = activityManager.willWait(activityID)
  protected[this] def willFinish(): Unit = activityManager.willFinish(activityID)

  protected[neko] def hasPendingMessages: Boolean

  final override def start(): Unit =
  {
    val wrapper = new Runnable {
      def run() {
        try {
          self.run ()
          self.onFinish ()
          activityManager.willFinish (activityID)
        } catch {
          case e @ ( _:BrokenBarrierException | _:ActivityAbortedError ) =>
            Console.err.println(s"$name has terminated abruptly.")
            self.onError (e)
          case e: Exception =>
            self.onError (e)
            activityManager.reset ()
            throw e
        }
      }
    }
    activityManager.willStart (activityID)
    system.executionContext.execute (wrapper)
  }
}
