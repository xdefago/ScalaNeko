---
title: Installing ScalaNeko Programming Environment on Visual Studio Code
kramdown:
  math_engine: mathjax
  syntax_highlighter: rouge
---
# Installing Programming Environment (Visual Studio Code)

This page explains how to install, from scratch, a working environment for developing ScalaNeko programs using Visual Studio Code.

The instructions below are tested on a Mac environment and require some adaptation for Linux or Windows.

## Preliminary Downloads

1. (Mac only) make sure that you have installed the Xcode command-line tools. E.g., with   
   `xcode-select --install`
1. Download and install [Visual Studo Code](https://code.visualstudio.com). (Mac Only) you can also do that using [Homebrew](https://brew.sh) with   
   `brew install vscode`.
1. Install `sbt` by [downloading it](https://www.scala-sbt.org) or via a package manager. (Mac only) homebrew solution is recommended for Macs:   
   `brew install sbt`

At some point, vscode might query you to install a version of JDK. I suggest that you do it, but you can also set it up to find a version already installed.


## Setting up Visual Studio Code

1. Open Visual Studio Code.
1. Install the following extensions:
    1. > Scala Syntax (official)
    1. > Scala (Metals)

## Create and Configure New Project (from Scratch)

### Create Project

In a terminal window,

1. Navigate to your project directory
1. Run the command:   
   `sbt new scala/hello-world.g8`
1. Give a name to your project when queried (e.g., `My Project`).
1. Open VScode if not already done:   
   `code my-project`
1. Or navigate to your project directory if you are already in VScode (e.g., `cd my-project`).
1. You should see the following:
    1. `build.sbt` is the construction file (like Makefile but smarter).
    1. `src/main/scala/` is the directory with the source code.
    1. `project/` is some meta information that you can genrally ignore.
    1. `.metal/` ditto.
    1. `.vscode/` ditto.
1. If using `git` you can safely add the latter two directories to your `.gitignore` file.


### Configure New Project

1. Open the file `build.sbt`. It is in the outline view on the left-hand side of the window.
2. Add the `libraryDependencies` and `resolvers` lines to the file _(make sure to keep an empty line between each line)_:   
  ```scala
  resolvers += "titech.c.coord" at "https://xdefago.github.io/ScalaNeko/sbt-repo/"
  
  libraryDependencies += "titech.c.coord" %% "scalaneko" % "0.24.0"
  ```
  You can also bump up the version number for Scala to `2.13.10` (latest version 2 at time of writing), but version 3 of Scala will probably not work with ScalaNeko.

3. Save the file
4. Open the terminal window (Menu: View > Terminal).
    1. Type `sbt compile` and see if there are any errors (if there are, you need to fix settings). 
    The first time will probably download lots of things and hence take a very long time.
    1. Type `sbt run` and see the hello world program running.


## Clone the Distributed Systems Course Template

### Setup the Project

1. Navigate to your project directory
1. Clone the project:   
   `git clone https://github.com/xdefago/class-da-prog.git`
1. Open VSCode:   
   `code class-da-prog`
    * It might take long the first time because Metals will download lots of dependencies
    * If queried about _"Import Build"_, do it and, although it will take even longer (several minutes), this will enable code sense for your project which is very useful to see infered types (among other things).

### Run the Hello World

1. Open the terminal window (Menu: View > Terminal).
    1. Type `sbt run`
    1. In the list of executable classes, pick `session0.a_hello_scala.HelloScala` (should probably be number `4`).
    1. You should see the output from the hello word:
  ```console
  [info] running (fork) session0.a_hello_scala.HelloScala 
  [info] Hello Scala's world!
  ```
2. To see the code itself, navigate on VScode to `src/main/scala/session0/a_hello_scala/HelloWorld.scala`   
  ```scala
package session0.a_hello_scala

object HelloScala extends App
{
    println("Hello Scala's world!")
}
```

### Run the Hello Neko

The next example creates a number of processes, each of which prints an hello message.

1. Navigate on VScode to `src/main/scala/session0/b_hello_neko/HelloNeko.scala`   
  ```scala
package session0.b_hello_neko

import neko._

class Hello(p: ProcessConfig) extends ActiveProtocol(p, "hello")
{
  def run(): Unit = {
    println(s"Process ${me.name} says: 'Hello Neko World!'")
  }
}

object HelloNeko
  extends Main(topology.Clique(5))(
    ProcessInitializer { p => new Hello(p) }
  )
```
2. Run the example (alternative method):
    1. In the terminal, launche the sbt interactive shell: `sbt`
    1. In the sbt shell, type `run` (and select the class)
    1. OR, type `runMain session0.b_hello_neko.HelloNeko` to directly run the class.
3. You should observe the following (or similar):   
```shell
sbt:DistribAlgo> runMain session0.b_hello_neko.HelloNeko
[info] compiling 1 Scala source to ...
[info] running (fork) session0.b_hello_neko.HelloNeko 
[info] Process p2 says: 'Hello Neko World!'
[info] Process p0 says: 'Hello Neko World!'
[info] Process p4 says: 'Hello Neko World!'
[info] Process p3 says: 'Hello Neko World!'
[info] Process p1 says: 'Hello Neko World!'
[success] Total time: 4 s, completed ...
sbt:DistribAlgo> 
```

Alternatively, you can also use [IntelliJ IDEA](idea)
