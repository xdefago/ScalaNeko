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

import java.io.{ PrintStream, PrintWriter }
import java.nio.file.{ Files, Paths }

import neko._
import neko.topology.Topology
import neko.util.Time

import scala.reflect.ClassTag


trait EventTracer
{
  def send(at: Time, by: PID, who: NamedEntity)(event: Event)

  def deliver(at: Time, by: PID, who: NamedEntity)(event: Event)

  def SEND(at: Time, by: PID, who: NamedEntity)(event: Event)

  def DELIVER(at: Time, by: PID, who: NamedEntity)(event: Event)

  def register(entity: NamedEntity) = { entities += entity }
  
  protected[this] var entities = Set.empty[NamedEntity]
  
  var protocols: Seq[String] = Seq()
  var protocolImpls: Map[PID, Seq[Protocol]] = Map()
  var mappingMessageIDToSender: Map[ID[_], Any] = Map()

  val ru = scala.reflect.runtime.universe
  val m = ru.runtimeMirror(getClass.getClassLoader)

  class MirrorAndAny(val mirror: ru.MethodMirror, var any: Option[Any])
  var mirrorLists: Map[PID, List[((String,String), MirrorAndAny)]] = Map().withDefaultValue(List.empty)

  def setTrace[A: ru.TypeTag : ClassTag](a: A, pid: PID, protoName: String, name: String = null): Unit = {

    val im = m.reflect(a)

    val ruType = ru.typeOf[A]
    // extracting getters for var and non-constructor vals
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
  
  def flush() = {}
}

object EventTracer
{
}

trait EventFormatter
{
  def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event): String
  def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event, who: Any): String
  def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event, whoCode: Int): String
  def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event, whoCode: String): String
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
  
  def default: EventFormatter = SimpleEventFormatter
  
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
    def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event, whoCode: Int): String = {
      val sKind  = formatKind(kind)
      val sTime  = formatTime(time)
      val sEvent = formatEvent(ev)
      s"${by.name} $whoCode $sKind $sTime $sEvent"
    }
    def stringFor(kind: EventFormatter.EventKind, time: Time, by: PID, ev: Event, whoStr: String): String = {
      val sKind  = formatKind(kind)
      val sTime  = formatTime(time)
      val sEvent = formatEvent(ev)
      s"${by.name} $whoStr $sKind $sTime $sEvent"
    }
  }
}

object ConsoleEventTracer extends EventTracer
{
  import EventFormatter._

  def send(at: Time, by: PID, who: NamedEntity)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      println(EventFormatter.default.stringFor(SND, at, by, event))
    }
  }
  def deliver(at: Time, by: PID, who: NamedEntity)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      println(EventFormatter.default.stringFor(SND, at, by, event))
    }
  }
  def SEND(at: Time, by: PID, who: NamedEntity)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      synchronized {
        mappingMessageIDToSender = mappingMessageIDToSender.updated(event.id, who)
      }
      println(EventFormatter.default.stringFor(RCV, at, by, event))
    }
  }
  def DELIVER(at: Time, by: PID, who: NamedEntity)(event: Event) = {
    if (who.isInstanceOf[neko.network.Network]) {
      synchronized {
        mappingMessageIDToSender = mappingMessageIDToSender.updated(event.id, who)
      }
      println(EventFormatter.default.stringFor(RCV, at, by, event))
    }
  }
}

case class SingleFileTracer(topology: Topology, out: PrintStream)
  extends EventTracer
{
  import SingleFileTracer._
  
  private var events = List.empty[EventInfo]

  def send(at: Time, by: PID, who: NamedEntity)(event: Event) =
    synchronized { events +:= EventSnd(at, by, who, event) }
  
  def deliver(at: Time, by: PID, who: NamedEntity)(event: Event) =
    synchronized { events +:= EventDlv(at, by, who, event) }
  
  def SEND(at: Time, by: PID, who: NamedEntity)(event: Event) =
    synchronized { events +:= EventSEND(at, by, who, event) }
  
  def DELIVER(at: Time, by: PID, who: NamedEntity)(event: Event) =
    synchronized { events +:= EventDELIVER(at, by, who, event) }
  
  private val protocolPattern = """.*:πρ\[([^\]]*)\].*""".r
  private val networkPattern = """[\s\S]*network[\s\S]*""".r
  private def nameOf(str: String) =
    str match {
      case protocolPattern(name) => name
      case networkPattern() => "network"
    }
  
  override def flush() = printTo(this.out)
  
  private def printTo(out: PrintStream): Unit =
  {
    // find all connected entities by transitivity
    var hasChanged = false
    do {
      hasChanged = false
      for {
        entity <- entities
        sender <- entity.senderOpt
        if ! entities.contains(sender)
      } {
        entities += sender
        hasChanged = true
      }
    } while (hasChanged)
  
    val entitiesByPIDName =
      entities
        .groupBy(_.context)
        .mapValues { _.groupBy(_.simpleName) }
    
    val nameCountByPID =
      entitiesByPIDName
        .mapValues( _.mapValues(_.size) )

    val nameLookupByPID =
      entitiesByPIDName
        .mapValues {
          entitiesByName =>
            for {
              (name, entities) <- entitiesByName
              (entity, idx)    <- entities.zipWithIndex
              uniqueName = s"${name }_$idx"
              nickname = if (entities.size == 1) name
              else uniqueName
            } yield entity -> nickname
        }
    
    val protocolNames = {
      for {
        (_, entities) <- nameLookupByPID
        (_, name)  <- entities
      } yield name
    }.toSet
    
    val nameLookup = protocolNames.toIndexedSeq
    val reverseNameLookup = nameLookup.zipWithIndex.map{ case (name, idx) => name -> idx }.toMap

    val connections = {
      for {
        context <- nameLookupByPID.keys
        pid <- context
      } yield pid -> {
          for {
            (proto, protoName) <- nameLookupByPID(context)
            sender <- proto.senderOpt
            senderName = nameLookupByPID(context).getOrElse(sender, sender.simpleName)
          } yield protoName -> senderName
        }
    }.toMap
    
    
    // print topology
    out.println(s"N=${topology.size}")
    out.println(s"topology=${topology.underlying.edges.mkString("", "#", "#")}")
    
    // print protocols lookup
    out.println(s"protocols=${nameLookup.size}")
    for ((name, idx) <- nameLookup.zipWithIndex) {
      out.println(s"$idx: $name")
    }
    
    // print protocol connections
    val encodedConnections =
      connections.keys.toSeq.sortWith(_ < _).map { pid =>
        {
          for ((fromName, toName) <- connections(pid)) yield {
            val fromID = reverseNameLookup(fromName)
            val toID = reverseNameLookup(toName)
            s"($fromID,$toID)"
          }
        }.mkString("")
      }.mkString("/")
    
    out.println(encodedConnections)
    
    for {
      pid <- connections.keys.toSeq.sortWith(_ < _)
      (fromName, toName) <- connections(pid)
      fromID = reverseNameLookup(fromName)
      toID   = reverseNameLookup(toName)
    } {
      out.println(s"($fromID,$toID) == $fromName --> $toName")
    }
    
    // print state variables (id, name)
    
    // print events
    val events = this.events.reverse.toIndexedSeq
    
    val encoders =
      nameLookupByPID.mapValues (
        _.mapValues (reverseNameLookup).withDefaultValue(-1)
      )
    
    for (event <- events) {
      out.println(event.format(encoders(event.who.context)))
    }
  }
}

object SingleFileTracer
{
  trait EventInfo
  {
    def at: Time
    def by: PID
    def who: NamedEntity
    def ev: Event
    def kind: EventFormatter.EventKind
    
    def format(encoder: NamedEntity=>Int): String = {
      val whoCode = encoder(who)
      val whoString = if (whoCode >= 0) whoCode.toString else s"<<${who.name}>>"
      EventFormatter.default.stringFor(kind, at, by, ev, whoString)
    }
  }
  case class EventSnd(at: Time, by: PID, who: NamedEntity, ev: Event) extends EventInfo
  {
    def kind = EventFormatter.sSend
  }
  case class EventSEND(at: Time, by: PID, who: NamedEntity, ev: Event) extends EventInfo
  {
    def kind = EventFormatter.lSend
  }
  case class EventDlv(at: Time, by: PID, who: NamedEntity, ev: Event) extends EventInfo
  {
    def kind = EventFormatter.sDelv
  }
  case class EventDELIVER(at: Time, by: PID, who: NamedEntity, ev: Event) extends EventInfo
  {
    def kind = EventFormatter.lDelv
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

  def send(at: Time, by: PID, who: NamedEntity)(event: Event) =
    synchronized {
      initFile()
      val proto = usingName(who.toString)
      if (traceName.isEmpty || traceName.get.contains(proto)) {
        val str = stringFor(sSend, at, by, event, proto)
        printFile(str.length + "," + str + reflect(by, who.toString))
        //eventDB.addEvent(EventForDB(by.name, protocolDB.getID(proto), Time.formatTimeSeconds(at), sSend.name, messageDB.getID(event.toString), id, event.messageID))
      }
    }
  
  def deliver(at: Time, by: PID, who: NamedEntity)(event: Event) =
    synchronized {
      initFile()
      val proto = usingName(who.toString)
      if (traceName.isEmpty || traceName.get.contains(proto)) {
        val str = stringFor(sDelv, at, by, event, proto)
        printFile(str.length + "," + str + reflect(by, who.toString))
        //eventDB.addEvent(EventForDB(by.name, protocolDB.getID(proto), Time.formatTimeSeconds(at), sDelv.name, messageDB.getID(event.toString), id, event.messageID))
      }
    }
  
  def SEND(at: Time, by: PID, who: NamedEntity)(event: Event) =
    synchronized {
      initFile()
      val proto = usingName(who.toString)
      if (traceName.isEmpty || traceName.get.contains(proto)) {
        val str = stringFor(lSend, at, by, event, proto)
        printFile(str.length + "," + str + reflect(by, who.toString))
        //eventDB.addEvent(EventForDB(by.name, protocolDB.getID(proto), Time.formatTimeSeconds(at), lSend.name, messageDB.getID(event.toString), id, event.messageID))
      }
    }
  
  def DELIVER(at: Time, by: PID, who: NamedEntity)(event: Event) =
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
        //val database = new File(s"./TraceLog/$filename.mv.db")
        //database.delete()
        protocolDB.initiate()
        variableDB.initiate()
        //eventDB.initiate()
        //messageDB.initiate()
        var staticBuffer = ""
        staticBuffer += protocolImpls.size.toString + "\n"
        var buffer: String = ""
        for (key <- protocolImpls.keys.toList.sortWith(_ < _)) {
          var buf: String = ""
          protocolImpls(key).map{
            p =>
              /*
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
              */
              "DUMMY" // TODO: fixme
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

      case Some(_) => /* ignore */
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
  def send(at: Time, by: PID, who: NamedEntity)(event: Event) = {}
  def deliver(at: Time, by: PID, who: NamedEntity)(event: Event) = {}
  def SEND(at: Time, by: PID, who: NamedEntity)(event: Event) = {}
  def DELIVER(at: Time, by: PID, who: NamedEntity)(event: Event) = {}
}
