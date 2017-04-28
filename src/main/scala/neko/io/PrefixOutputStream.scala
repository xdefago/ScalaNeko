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
