---
title: Installing ScalaNeko Programming Environment
kramdown:
  math_engine: mathjax
  syntax_highlighter: rouge
---
# Installing Programming Environment

This page explains how to install, from scratch, a working environment for developing ScalaNeko programs.


## Preliminary Downloads

1. (Mac only) On Mac OS X, you will possibly be required to install the execution environment for Java 1.6 as well. To do so, open the `Terminal` application, type `java -version`. If Java is not installed, you will be prompted to install it (just follow the instruction which will guide you through the download and install process). If it is already installed, you're all good so far and can simply go to the next step. 

2. Download and install **Java SE Development Kit** _(JDK 8)_ from Oracle at <http://www.oracle.com/technetwork/java/javase/downloads/index.html>. You need to install the JDK (not the JRE).

3. Download and install **IntelliJ IDEA** _(version 2017.x)_ from JetBrains at <https://www.jetbrains.com/idea/download/>. If unsure, select the Community Edition.

	

## Setting up IDEA

1. Launch the IDEA application.

2. If asked by the OS if it is OK to open, click on **"Open"**.

3. Select _"Do not import from previous version"_ if it's installed for the first time.

4. Answer preliminary settings questions:

	1. **Set UI Theme as preferred:** Either are fine. Click **"next"**.
	2. **Tune IDEA:** Default values are fine. Click **"next"**.
	3. **Download featured plugins:** Under the "scala plugin", click **"Install"** and wait until it's done. Then, click **"next"**.

5. Start the IntelliJ application and do some additional setup as follows:

	1. Sometimes, when you start IntelliJ for the first time, it tells you that it has no JDK and asks you to provide one. You need to tell IntelliJ where to find the JDK that you have installed previously. You can follow the instructions at <https://www.jetbrains.com/idea/help/configuring-global-project-and-module-sdks.html#d1278485e16>. In many cases, you only need to open the dialog and click OK, as IntelliJ will search for an appropriate location.
	2. From the main dialog "Welcome to IntelliJ IDEA", proceed as follows:
		1. Select **Configure** and **Plugins**.
		2. Click on button **Browse repositories...**
		3. In the search box, type `scala`
		4. Find plugin called `SBT` and click **Install plugin**.
		5. If not already installed, find plugin called `Scala` and click **Install plugin**
		6. Click **Close**, then **OK**, then **Restart**

## Create and Configure New Project

### Create Project

1. Menu **"File"** -> **"New Project..."**

2. Select **Scala** (left side), then **SBT** (right side).

3. Configure project details:
	1. Set project name _(e.g, `DistribCourse`)_
	2. Set JDK
		* If the JDK is missing/empty under that item, then proceed as follows:
			- click **"New..."**
			- Then, simply click **"OK"** and IDEA should find the previously installed JDK automatically.
			- If not, then click again on **"New..."** and navigate to the directory where the JDK has been installed on the system.

4. Click **"Finish"**.

### Configure New Project

1. Open the file `build.sbt`. It is in the outline view on the left side of the window.

2. Add the `libraryDependencies` and `resolvers` lines to the file _(make sure to keep an empty line between each line)_:

```scala
name := "DistribCourse"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "titech.c.coord" at "https://github.com/xdefago/sbt-repo/raw/master/"

libraryDependencies += "titech.c.coord" %% "scalaneko" % "0.18.0"
```

3. Save the file

4. Wait until a directory `src` appears in the project. Normally, this can take several minutes, as all libraries are being downloaded. You can check the status bar at the bottom to see if IntelliJ is still working. Once it has finished, the directory `src` should finally appear. If it does not appear, then you can do the following:

	1. Click on **SBT Console** (at bottom left) 
	2. In the SBT console window, click on the green triangle.
	3. Wait for the console to start.
	4. At the prompt, type `reload`
	5. Then type `update`

	After that, if the `src` directory is still not there, then try the following:
	
	1. Click on **SBT** (at top right)
	2. In the new window, click on the two circular arrows (Refresh all SBT projects).
	3. Wait again.


## Write the Hello World

Let's make a simple ScalaNeko program to illustrate the settings required for execution from within the IDEA.

1. Select `src/main/scala` (outline view in the left pane of the project window)

2. Menu "File" -> "New..." -> "Scala Class"

3. Name: `p01.HelloWorld`; Kind: **Object**

4. Click "OK"

5. Edit the file `HelloWorld.scala` as follows:

```scala
package p01

import neko._

class MyProcess(c: ProcessConfig) extends ActiveProtocol(c)
{
  def run (): Unit =
  {
    println(s"Hello World! ${me.name}")
  }
}

object HelloWorld
  extends Main (topology.Clique(2)) (
    ProcessInitializer { p=> new MyProcess(p) }
  )
```

## Compile and Run the Program

1. Click on **"SBT Console"**

2. Click on green arrow to start the console

3. Type `compile`

4. Type `run`

You should see the following message

```shell
> run
[info] Running p01.HelloWorld 
Hello World! p0
Hello World! p1
[success] Total time: 3 s, completed ...
> 
```

The program was configured with one single process, which prints the message `"Hello neko"`.

Before going any further with the program, we modify the program to display trace information and see what happens under the hood.

1. Update the program by changing the log level to `TRACE` as follows:

```scala
object HelloWorld
  extends Main (
    topology.Clique(2),
    logLevel=ch.qos.logback.classic.Level.ALL
  ) (
    ProcessInitializer { p=> new MyProcess(p) }
  )
```

2. Go back to the SBT console and `run`.

The following information should now be displayed:

```shell
> run
[info] Running p01.HelloWorld 
20:00:38.925 [run-main-3] INFO  neko.NekoMain - Starting
20:00:38.937 [run-main-3] INFO  neko.sim.NekoSimSystem - INIT: creating networks
20:00:38.942 [run-main-3] INFO  neko.sim.NekoSimSystem - INIT: creating processes
20:00:38.953 [run-main-3] TRACE neko.kernel.ActivityManager - registerActivity(p0:πρ[app])
20:00:38.954 [run-main-3] TRACE neko.kernel.ActivityManager - Activity registry: ActivityID(0) -> p0:πρ[app]
20:00:38.955 [run-main-3] TRACE neko.kernel.ActivityManager - registerActivity(p1:πρ[app])
20:00:38.955 [run-main-3] TRACE neko.kernel.ActivityManager - Activity registry: ActivityID(1) -> p1:πρ[app]
20:00:38.955 [run-main-3] INFO  neko.sim.NekoSimSystem - INIT: registering processes to networks
20:00:38.956 [run-main-3] INFO  neko.sim.NekoSimSystem - INIT: starting networks
20:00:38.956 [run-main-3] INFO  neko.sim.NekoSimSystem - INIT: prestarting all processes
20:00:38.957 [run-main-3] INFO  neko.sim.NekoSimSystem - INIT: readying simulator
20:00:38.957 [run-main-3] TRACE neko.kernel.ActivityManager - registerAction(...) -> 1 actions registered
20:00:38.957 [run-main-3] TRACE neko.kernel.ActivityManager - start()
20:00:38.958 [run-main-3] TRACE neko.kernel.ActivityManager - REGISTERED: p0:πρ[app], p1:πρ[app]
20:00:38.960 [run-main-3] INFO  neko.sim.NekoSimSystem - INIT: starting processes
20:00:38.961 [run-main-3] TRACE neko.kernel.ActivityManager - willStart(p0:πρ[app])
20:00:38.962 [run-main-3] TRACE neko.kernel.ActivityManager - willStart(p1:πρ[app])
Hello World! p0
Hello World! p1
20:00:38.965 [pool-11-thread-1] TRACE neko.kernel.ActivityManager - willFinish(p0:πρ[app]). Unfinished = 
20:00:38.965 [pool-11-thread-2] TRACE neko.kernel.ActivityManager - willFinish(p1:πρ[app]). Unfinished = 
20:00:38.967 [pool-11-thread-2] TRACE neko.kernel.ActivityManager - Scheduler actions : p0:πρ[app]/Finished, p1:πρ[app]/Finished
20:00:38.967 [pool-11-thread-2] TRACE neko.kernel.ActivityManager - All finished
20:00:38.972 [pool-11-thread-2] INFO  neko.sim.NekoSimSystem - Simulation ended normally (at 0s)
20:00:38.972 [run-main-3] INFO  neko.sim.NekoSimSystem - JOIN: All activities finished
20:00:38.973 [run-main-3] INFO  neko.NekoMain - Exiting
[success] Total time: 3 s, completed Jun 16, 2015 8:00:38 PM
```

This provides information showing the initialization sequence of ScalaNeko before the single process is executed and displays its message to the console.

The rest will be done together during the lecture and possibly detailed in other documents later. Meanwhile, you can continue from here by browsing the [API documentation](api/neko/) in which you can find many examples.


