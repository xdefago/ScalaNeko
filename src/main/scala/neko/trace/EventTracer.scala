/**
 *
 * Copyright 2015 Xavier Defago & Naoyuki Onuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by IntelliJ IDEA.
 * User: defago
 * Date: 31/05/15
 * Time: 13:55
 *
 */
package neko.trace

import java.io.{ File, PrintWriter }
import java.nio.file.{ Files, Paths }

import neko._
import neko.topology.Topology
import neko.util.Time

import scala.reflect.ClassTag


trait EventTracer
{
  def send(at: Time, by: PID, who: Sender)(event: Event)

  def deliver(at: Time, by: PID, who: Receiving)(event: Event)

  def SEND(at: Time, by: PID, who: Receiving)(event: Event)

  def DELIVER(at: Time, by: PID, who: Sender)(event: Event)

  var protocols: Seq[String] = Seq()
  var protocolImpls: Map[PID, Seq[ProtocolImpl]] = Map()
  var mappingMessageIDToSender: Map[ID[_], Any] = Map()

  val ru = scala.reflect.runtime.universe
  val m = ru.runtimeMirror(getClass.getClassLoader)

  class MirrorAndAny(val mirror: ru.MethodMirror, var any: Option[Any])
  var mirrorLists: Map[PID, List[((String,String), MirrorAndAny)]] = Map().withDefaultValue(List.empty)

  def setTrace[A: ru.TypeTag : ClassTag](a: A, pid: PID, protoName: String, name: String = null): Unit = {

    val im = m.reflect(a)

    val ruType = ru.typeOf[A]
    // コンストラクタの変数でないvalとvarのgetterの取得
    var getters =
      ruType.decls.filter(_.isMethod).map(_.asMethod)
        .filter(t => !t.isParamAccessor && t.isGetter)

    if (name != null) {
      getters = getters.filter(_.name.toString == name)
      if (getters.isEmpty) println(s"""[$protoName] :> the field "$name" can't be found""")
    }

    getters.foreach { methodSymbol =>
      val name = methodSymbol.name.toString
      try {
        val mm = im.reflectMethod(methodSymbol)

        val protocolPattern = """[\s\S]+πρ\[([\s\S]+)\][\s\S]+""".r
        protoName match {
          case protocolPattern(protoname) =>
            synchronized {
              //println((protoname, ProtocolDB.getID(protoname)))
              mirrorLists = mirrorLists.updated(pid, mirrorLists(pid) :+ ((protoname, name), new MirrorAndAny(mm, None)))
            }
          case protoname: String =>
            synchronized {
              //println((protoname, ProtocolDB.getID(protoname)))
              mirrorLists = mirrorLists.updated(pid, mirrorLists(pid) :+ ((protoname, name), new MirrorAndAny(mm, None)))
            }
        }
      } catch {
        case _: Throwable => println(s"""[$protoName] :> the field "$name" can't be traced""")
      }
    }

  }

  //private var lastReflect: Map[PID, ListSet[Option[Any]]] = Map().withDefaultValue(ListSet())
  def reflect(pid: PID, protoName: String): String = {
    if (mirrorLists.contains(pid))
      mirrorLists(pid)
        .map{ t =>
          val value = t._2.mirror.apply()
          val str =
            if (t._2.any.contains(value)) "n"
            else s"${value.toString.length},${value.toString}"
          t._2.any = Some(value)
          str
        }
        .mkString("", "", "")
    else ""
  }
}

object EventTracer
{
}

trait EventFormatter
{
  def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event): String
  def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event, who: Any): String
}

object EventFormatter
{
  sealed trait EventKind { def name: String }
  case object SND   extends EventKind { val name = "SND" }
  case object RCV   extends EventKind { val name = "RCV"}
  case object sSend extends EventKind { val name = "snd" }
  case object lSend extends EventKind { val name = "SND" }
  case object sDelv extends EventKind { val name = "dlv" }
  case object lDelv extends EventKind { val name = "DLV" }
  case object Sig   extends EventKind { val name = "SIG"}

  object SimpleEventFormatter extends EventFormatter
  {
    protected def formatTime(time: Time): String = Time.formatTimeSeconds(time)
    protected def formatKind(kind: EventFormatter.EventKind): String = kind.name
    protected def formatClass(clazz: Class[_]): String = clazz.getSimpleName
    protected def formatEvent(ev: Event): String = ev.toString

    def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event): String =
    {
      val sKind = formatKind(kind)
      val sTime = formatTime(time)
      val sEvent = formatEvent(ev)
      s"[${by.name}] $sTime $sKind $sEvent"
    }
    def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event, who: Any): String = {
      val sKind  = formatKind(kind)
      val sTime  = formatTime(time)
      val sID    = who match { case p:Protocol => p.id.toString ; case _ => "0" }
      val mID    = ev  match { case m:Message  => m.id.toString ; case _ => "MessageID(0)"}
      val sEvent = formatEvent(ev) + s" commID($sID) $mID"
      s"[${by.name}] [$who] $sTime $sKind $sEvent"
    }
  }
}

object ConsoleEventTracer extends EventTracer
{
  import EventFormatter.SimpleEventFormatter.stringFor
  import EventFormatter._

  def send(at: Time, by: PID, who: Sender)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      println(stringFor(SND, at, by, event))
    }
  }
  def deliver(at: Time, by: PID, who: Receiving)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      println(stringFor(SND, at, by, event))
    }
  }
  def SEND(at: Time, by: PID, who: Receiving)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      synchronized {
        mappingMessageIDToSender = mappingMessageIDToSender.updated(event.id, who)
      }
      println(stringFor(RCV, at, by, event))
    }
  }
  def DELIVER(at: Time, by: PID, who: Sender)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      synchronized {
        mappingMessageIDToSender = mappingMessageIDToSender.updated(event.id, who)
      }
      println(stringFor(RCV, at, by, event))
    }
  }
}


case class FileEventTracer(filename: String, topology: Topology)
  extends EventTracer
{
  import EventFormatter.SimpleEventFormatter.stringFor
  import EventFormatter._

  var file: Option[PrintWriter] = None

  /* nicknames of tracing protocol */
  val traceName: Option[Set[String]] = None

  val protocolDB = new ProtocolDB(filename)
  val variableDB = new VariableNameDB(filename)
  //val eventDB = new EventDB(filename)
  //val messageDB = new MessageDB(filename)

  var usingNameBy: Map[PID, Map[String, String]] = Map().withDefaultValue(Map())
  var usingName: Map[String, String] = Map()
  val protocolPattern = """[\s\S]*πρ\[([\s\S]*)\][\s\S]*""".r
  val networkPattern = """[\s\S]*network[\s\S]*""".r

  def send(at: Time, by: PID, who: Sender)(event: Event) =
    synchronized {
      initFile()
      val proto = usingName(who.toString)
      if (traceName.isEmpty || traceName.get.contains(proto)) {
        val str = stringFor(sSend, at, by, event, proto)
        printFile(str.length + "," + str + reflect(by, who.toString))
        //eventDB.addEvent(EventForDB(by.name, protocolDB.getID(proto), Time.formatTimeSeconds(at), sSend.name, messageDB.getID(event.toString), id, event.messageID))
      }
    }
  
  def deliver(at: Time, by: PID, who: Receiving)(event: Event) =
    synchronized {
      initFile()
      val proto = usingName(who.toString)
      if (traceName.isEmpty || traceName.get.contains(proto)) {
        val str = stringFor(sDelv, at, by, event, proto)
        printFile(str.length + "," + str + reflect(by, who.toString))
        //eventDB.addEvent(EventForDB(by.name, protocolDB.getID(proto), Time.formatTimeSeconds(at), sDelv.name, messageDB.getID(event.toString), id, event.messageID))
      }
    }
  
  def SEND(at: Time, by: PID, who: Receiving)(event: Event) =
    synchronized {
      initFile()
      val proto = usingName(who.toString)
      if (traceName.isEmpty || traceName.get.contains(proto)) {
        val str = stringFor(lSend, at, by, event, proto)
        printFile(str.length + "," + str + reflect(by, who.toString))
        //eventDB.addEvent(EventForDB(by.name, protocolDB.getID(proto), Time.formatTimeSeconds(at), lSend.name, messageDB.getID(event.toString), id, event.messageID))
      }
    }
  
  def DELIVER(at: Time, by: PID, who: Sender)(event: Event) =
    synchronized {
      initFile()
      val proto = usingName(who.toString)
      if (traceName.isEmpty || traceName.get.contains(proto)) {
        val str = stringFor(lDelv, at, by, event, proto)
        printFile(str.length + "," + str + reflect(by, who.toString))
        //eventDB.addEvent(EventForDB(by.name, protocolDB.getID(proto), Time.formatTimeSeconds(at), lDelv.name, messageDB.getID(event.toString), id, event.messageID))
      }
    }
  

  def initFile(): Unit =
  {
    file match {
      case None =>
        val dir = Paths.get("TraceLog")
        if(Files.notExists(dir)) Files.createDirectory(dir) // mkdir
        file = Some(new PrintWriter("TraceLog/" + filename + ".trace"))
        val database = new File(s"./TraceLog/$filename.mv.db")
        database.delete()
        protocolDB.initiate()
        variableDB.initiate()
        //eventDB.initiate()
        //messageDB.initiate()
        var staticBuffer = ""
        staticBuffer += protocolImpls.size.toString + "\n"//file.get.write(protocolImpls.size.toString + ",")
        var buffer: String = ""
        for (key <- protocolImpls.keys.toList.sortWith(_ < _)) {
          var buf: String = ""
          protocolImpls(key).map{
            p =>
              var proto1 = p.toString match {
                case protocolPattern(name) => name
                case networkPattern() => "network"
              }
              var i = 1
              while (usingNameBy(key).exists(s => s._1 != p.toString && s._2 == proto1)) {
                proto1 = proto1 + "_" + i.toString
                i += 1
              }
              usingNameBy = usingNameBy.updated(key, usingNameBy(key).updated(p.toString, proto1))
              var proto2 = p.sender.toString match {
                case protocolPattern(name) => name
                case networkPattern() => "network"
              }
              i = 1
              while (usingNameBy(key).exists(s => s._1 != p.sender.toString && s._2 == proto2)) {
                proto2 = proto2 + "_" + i.toString
                i += 1
              }
              usingNameBy = usingNameBy.updated(key, usingNameBy(key).updated(p.sender.toString, proto2))
              val id1 = protocolDB.getID(proto1)
              val id2 = protocolDB.getID(proto2)
              //proto1.length + "-" + proto1 + proto2
              s"($id1,$id2)"
          }.foreach {
            str =>
              buf = buf + str
          }
          buffer = buffer + (buf + "/")
        }

        staticBuffer += buffer+ "\n"

        var buf: String = ""
        for (pid <- mirrorLists.keys.toList.sortWith(_ < _)) {
          buffer = mirrorLists(pid).map{
            p =>
              val id1 = protocolDB.getID(p._1._1)
              val id2 = variableDB.getID(p._1._2)
              s"($id1,$id2)"
          }.mkString("")
          buf = buf + (buffer + "/")
        }
        staticBuffer += buf + "\n"

        staticBuffer += topology.underlying.edges.mkString("", "#", "#")

        usingNameBy.foreach(pair => usingName = usingName ++ pair._2)

        file.get.write(staticBuffer)

      case Some(_) =>
    }
  }

  def printFile(s: String) = {
    initFile()
    file.get.write("\n" + s.length + "," + s)
    file.get.flush()
  }
}


object NullEventTracer extends EventTracer
{
  def send(at: Time, by: PID, who: Sender)(event: Event) = {}
  def deliver(at: Time, by: PID, who: Receiving)(event: Event) = {}
  def SEND(at: Time, by: PID, who: Receiving)(event: Event) = {}
  def DELIVER(at: Time, by: PID, who: Sender)(event: Event) = {}
}
