import scala.util.matching.Regex.Match

name := "ScalaNeko"

organization := "titech.c.coord"

version := "0.21.0"

scalaVersion := "2.12.11"

initialCommands in console := "import neko._"

scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits", "-author", "-diagrams")

scalacOptions in (Compile,doc) ++= Seq("-skip-packages", "experimental:nekox")

scalacOptions += "-deprecation"

libraryDependencies ++= {
  lazy val graphV = "1.13.1"
  lazy val logbackV = "1.2.3"
  lazy val configV = "1.4.0"
  lazy val loggingV = "3.9.2"
  lazy val scalaTestV = "3.1.1"
  Seq(
    /*
     *  Configuration
     */
    "com.typesafe" % "config" % configV,
    /*
     *  Logging
     */
    "com.typesafe.scala-logging" %% "scala-logging" % loggingV,
    "ch.qos.logback" % "logback-classic" % logbackV,
    /*
     *  Graphs
     */
    "org.scala-graph" %% "graph-core" % graphV,
    /*
     *  Testing
     */
    "org.scalactic" %% "scalactic" % scalaTestV,
    "org.scalatest" %% "scalatest" % scalaTestV % "test"
  )
}


// libraryDependencies += "com.h2database" % "h2" % "1.4.194"

//
// Settings for JavaFX/ScalaFX
//

libraryDependencies += "org.scalafx" %% "scalafx" % "12.0.2-R18"

//unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/ext/jfxrt.jar"))

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

// Add dependency on JavaFX libraries, OS dependent
lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map( m =>
  "org.openjfx" % s"javafx-$m" % "12.0.2" classifier osName
)

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


// enablePlugins(SiteScaladocPlugin)


autoAPIMappings := true

// builds -doc-external-doc
apiMappings += (
    file("/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/jre/lib/rt.jar") ->
    url("https://docs.oracle.com/en/java/javase/11/docs/api")
)

lazy val fixJavaLinksTask = taskKey[Unit](
    "Fix Java links - replace #java.io.File with ?java/io/File.html"
)

val fixJavaLinks: Match => String = m =>
    m.group(1) + "?" + m.group(2).replace(".", "/") + ".html"

val javadocApiLink = """\"(https://docs.oracle.com/en/java/javase/11/docs/api/index\.html)#([^"]*)\"""".r

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

// fixJavaLinksTask <<= fixJavaLinksTask triggeredBy (doc in Compile)
fixJavaLinksTask := (fixJavaLinksTask triggeredBy (doc in Compile) ).value
