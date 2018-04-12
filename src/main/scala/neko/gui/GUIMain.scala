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
package neko.gui

import ch.qos.logback.classic.Level
import javafx.application.Platform
import neko._
import neko.topology._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.{ Tab, TabPane }

class GUIMain(
    val topology : Topology,
    logLevel : Level = Level.ERROR,
    logFile  : Option[String] = None,
    withTrace : Boolean = false
)(initializer: ProcessInitializer) extends JFXApp
{
  stage = new PrimaryStage {
    title = "Console output"

    width  = 1000
    height = 750
    
    scene = new Scene {
      def sceneWidth  = this.width
      def sceneHeight = this.height
      def thisScene   = this
      content = new TabPane {
        prefWidth  <== sceneWidth
        prefHeight <== sceneHeight
        
        this +=
          new Tab {
            text = "processes"
            closable = false
            content  = new MultiprocessConsolePane(topology)(3)
            {
              prefWidth  <== sceneWidth
              prefHeight <== sceneHeight
            }
          }
       }
    }

    val main = new Main(topology, logLevel, logFile, withTrace)(initializer)
    
    Platform.runLater { ()=>
      main.main(Array.empty)
    }
  }
}
