import scala.util.matching.Regex.Match

name := "ScalaNeko"

organization := "jp.ac.titech.c.coord"

version := "0.15"

scalaVersion := "2.11.8"


initialCommands in console := "import neko._"

scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits")

libraryDependencies ++= Seq(
  /*
   *  Configuration
   */
  "com.typesafe" % "config" % "1.3.0",
  /*
   *  Logging
   */
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  /*
   *  Graphs
   */
  "com.assembla.scala-incubator" %% "graph-core" % "1.11.0"
)

// libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

publishTo := Some(Resolver.ssh("Defago at JAIST", "www.jaist.ac.jp", "public_html/sbt-repo") as "defago" withPermissions "0644")

// HAS ISSUES: publishTo := Some(Resolver.file("Defago at JAIST", Path.userHome / "Dropbox" / "Web" / "JAIST" / "public_html" / "sbt-repo"))


// TO USE:
//resolvers += "Defago at JAIST" at "https://www.jaist.ac.jp/~defago/sbt-repo"
//libraryDependencies += ("jp.ac.jaist.defago" %% "scalaneko" % "0.6.1")



autoAPIMappings := true

// builds -doc-external-doc
apiMappings += (
    file("/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/jre/lib/rt.jar") ->
    url("http://docs.oracle.com/javase/8/docs/api")
)

lazy val fixJavaLinksTask = taskKey[Unit](
    "Fix Java links - replace #java.io.File with ?java/io/File.html"
)

val fixJavaLinks: Match => String = m =>
    m.group(1) + "?" + m.group(2).replace(".", "/") + ".html"

val javadocApiLink = """\"(http://docs\.oracle\.com/javase/8/docs/api/index\.html)#([^"]*)\"""".r

fixJavaLinksTask := {
  println("Fixing Java links")
  val t = (target in (Compile, doc)).value
  (t ** "*.html").get.filter(hasJavadocApiLink).foreach { f =>
    println("fixing " + f)
    val newContent = javadocApiLink.replaceAllIn(IO.read(f), fixJavaLinks)
    IO.write(f, newContent)
  }
}

def hasJavadocApiLink(f: File): Boolean = (javadocApiLink findFirstIn IO.read(f)).nonEmpty

fixJavaLinksTask <<= fixJavaLinksTask triggeredBy (doc in Compile)
