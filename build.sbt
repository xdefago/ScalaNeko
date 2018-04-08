import scala.util.matching.Regex.Match

name := "ScalaNeko"

organization := "titech.c.coord"

version := "0.19.0-SNAPSHOT"

scalaVersion := "2.12.5"


initialCommands in console := "import neko._"

scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits", "-author", "-diagrams")

scalacOptions in (Compile,doc) ++= Seq("-skip-packages", "experimental:nekox")

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  /*
   *  Configuration
   */
  "com.typesafe" % "config" % "1.3.3",
  /*
   *  Logging
   */
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  /*
   *  Graphs
   */
  "org.scala-graph" %% "graph-core" % "1.12.3"
)


// libraryDependencies += "com.h2database" % "h2" % "1.4.194"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

//
// Settings for JavaFX/ScalaFX
//

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.144-R12"

unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/ext/jfxrt.jar"))

fork in run := true

//
// Settings for publishing to repository over github
//

publishTo := Some(
  Resolver.file("scalaneko", file(Path.userHome.absolutePath + "/GithubLocal/sbt-repo"))
)

// to use:
//   resolvers += "titech.c.coord" at "https://github.com/xdefago/sbt-repo/"
//   libraryDependencies += "titech.c.coord" %% "ocelot" % <version>



//publishTo :=
//  Some (
//    Resolver.ssh ("Defago at Tokyo Tech", "web-o1.noc.titech.ac.jp", "www/sbt-repo") as
//    "c0004" withPermissions "0644"
//  )

// TO USE:
//resolvers += "Defago at Tokyo Tech" at "http://www.coord.c.titech.ac.jp/sbt-repo"
//libraryDependencies += ("jp.ac.titech.c.coord.defago" %% "scalaneko" % "0.15")


//publishTo := Some(Resolver.ssh("Defago at JAIST", "www.jaist.ac.jp", "public_html/sbt-repo") as "defago" withPermissions "0644")

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
