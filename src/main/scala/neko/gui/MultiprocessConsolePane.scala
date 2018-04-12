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

import java.io.{ PrintWriter, Writer }

import neko.PID
import neko.topology.Topology
import scalafx.beans.property.StringProperty
import scalafx.scene.control.{ TextArea, Tooltip }
import scalafx.scene.layout.GridPane
import scalafx.scene.text.Font

class StringPropertyWriter(prop: StringProperty) extends Writer
{
  def flush () = {}
  def close () = {}
  
  def write (cbuf: Array[Char], off: Int, len: Int) =
  {
    val adding = cbuf.slice(off, off+len).mkString("")
    prop.value = prop.value + adding
  }
}

class WriterArea(val name: String = "") extends TextArea
{
  val out = new PrintWriter(new StringPropertyWriter(text))
}

/**
 * Created by defago on 11/05/2017.
 */
class MultiprocessConsolePane(topology: Topology)(columns: Int = 1) extends GridPane
{
  require (columns > 0)
  require (topology.size > 0)
  
  val pids = topology.processSet.toIndexedSeq.sorted
  val N = pids.size
  
  println(s"N = $N")
  
  val COLS = columns
  val ROWS = 1 + 1 + (N-1) / COLS
  
  private val areas: IndexedSeq[WriterArea] = {
    for {
      pid <- pids
      name = pid.name
    } yield
      new WriterArea(name)
      {
        promptText = s"$name > "
        editable = false
        font = Font("Menlo", 12.0)
        onMouseClicked = { ev => out.println(s"Mouse at (${ev.getSceneX }, ${ev.getSceneY })") }
      }
  }
  private val lookupPID  = pids.zipWithIndex.map { case (pid,i) => pid -> i }.toMap
  def out(pid: PID): PrintWriter = outOption(pid).get
  def outOption(pid: PID): Option[PrintWriter] =
    lookupPID.get(pid).map { i => areas(i).out }
  
  private def paneWidth  = width
  private def paneHeight = height
  
  for {
    (node, i) <- areas.zipWithIndex
    (col, row) = (i % COLS , i / COLS)
  } {
    node.tooltip = new Tooltip(node.name)
    node.prefWidth  <== paneWidth  / COLS
    node.prefHeight <== paneHeight / ROWS
    add(node, col, row)
  }
  
  val networkArea =
    new WriterArea("network")
    {
      promptText = s"$name > "
      editable = false
      font = Font("Menlo", 12.0)
      onMouseClicked = { ev => out.println(s"Mouse at (${ev.getSceneX }, ${ev.getSceneY })") }
      
      tooltip = new Tooltip(name)
      prefWidth  <== paneWidth
      prefHeight <== paneHeight / ROWS
    }
  add(networkArea, 0, ROWS-1, colspan = COLS, rowspan = 1)
  
  def outCommon: PrintWriter = networkArea.out
}
