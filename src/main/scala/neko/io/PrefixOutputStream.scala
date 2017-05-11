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

package neko.io

import java.io._


/**
 * Created by defago on 26/04/2017.
 */


class PrefixOutputStream(prefix: String, out: PrintStream)
  extends PrintStream(
    new OutputStream {
      val lineSep = scala.util.Properties.lineSeparator
      var cursor   = 0
      var pending  = true
    
      def write (b: Int): Unit =
      {
        if (pending) {
          pending = false
          out.print(prefix)
        }
        if (lineSep(cursor) == b) {
          cursor += 1
    
          if (cursor == lineSep.length) {
            cursor  = 0
            pending = true
            out.println()
          }
        }
        else
          out.write(b)
      }
    
      override def flush() =
      {
        cursor  = 0
        pending = true
        out.flush ()
      }
    }
  )
{
}
