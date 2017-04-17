package neko.trace

import java.sql.DriverManager


/**
  * Created by Onuki on 2017/03/06.
  */
class VariableNameDB(fileName: String) {

  def initiate(): Unit = {
    Class.forName("org.h2.Driver")
    val conn = DriverManager.getConnection(s"jdbc:h2:./TraceLog/$fileName", "sa", "")
    val stmt = conn.createStatement

    try {
      stmt.execute("""drop table variable if exists""")
      stmt.execute(
        """create table if not exists variable (
      |id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
      |name varchar(255))""".stripMargin)
    } finally {
      stmt.close()
      conn.close()
    }
  }

  def getID(name: String): Int = {
    Class.forName("org.h2.Driver")
    val conn = DriverManager.getConnection(s"jdbc:h2:./TraceLog/$fileName", "sa", "")
    val stmt = conn.createStatement

    try {
      val countResult = stmt.executeQuery(s"""select COUNT(*) as count from variable where name='$name'""")
      assert(countResult.next(), true)
      if (countResult.getInt("count") == 0) {
        stmt.execute(s"""insert into variable (name) values ('$name')""")
      }
      val result = stmt.executeQuery(s"""select id from variable where name='$name'""")
      assert(result.next(), true)
      result.getInt("id")
    } finally {
      stmt.close()
      conn.close()
    }
  }

  def getName(id: Int): String = {
    Class.forName("org.h2.Driver")
    val conn = DriverManager.getConnection(s"jdbc:h2:./TraceLog/$fileName", "sa", "")
    val stmt = conn.createStatement

    try {
      val countResult = stmt.executeQuery(s"""select COUNT(*) as count from variable where id='$id'""")
      assert(countResult.next(), true)
      if (countResult.getInt("count") == 0) {
        ""
      } else {
        val result = stmt.executeQuery(s"""select name from variable where id='$id'""")
        assert(result.next(), true)
        result.getString("name")
      }
    } finally {
      stmt.close()
      conn.close()
    }
  }

}
