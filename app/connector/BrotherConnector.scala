package connector

import scala.util.Try
import scala.concurrent.duration._
import play.Logger
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor._
import akka.util.ByteString
import rxtxio._
import Serial._
import models._
import Connector._

class BrotherConnector(port: String, serialManager: ActorRef, parser: String => Option[Connector.Event]) extends Actor with ActorLogging {
  private val (rawEnumerator, channel) = Concurrent.broadcast[ByteString]
  def lineEnumerator = {
    def takeLine = for {
      line <- Enumeratee.takeWhile[Char](_ != '\n') &>> Iteratee.getChunks
      _ <- Enumeratee.take(1) &>> Iteratee.ignore[Char]
    } yield line.mkString

    rawEnumerator &>
      Enumeratee.mapConcat(_.toSeq.map(_.toChar)) &>
      Enumeratee.grouped(takeLine)
  }
  def eventEnumerator = {
    lineEnumerator &> Enumeratee.mapFlatten(in => Enumerator(parser(in).toSeq: _*))
  }

  private val encoding = "ASCII"

  override def preStart = {
    serialManager ! Open(port, 115200)
    context.setReceiveTimeout(5.seconds)
  }

  override def receive = {
    case Opened(operator, `port`) =>
      log.info(s"Serial port connection to brother opened on $port")
      //Start sending parsed events to the listener
      val me = self
      eventEnumerator(Iteratee.foreach(me.tell(_, me)))
      //Start the pattern manager
      val patternManager = {
        val props = Props(new BrotherPatternManger(operator, encoding))
        context.actorOf(props)
      }
      //TODO add an actor that sleeps the pattern manager (turns off all solenoids) after
      // a sleep timeout
      context.setReceiveTimeout(Duration.Undefined)
      context become open(operator, patternManager)

    case CommandFailed(Open(port, _), error) =>
      throw new RuntimeException("Could not open serial port for brother", error)

    case ReceiveTimeout =>
      throw new RuntimeException("Could not open serial port for brother within timeout")
  }

  def open(operator: ActorRef, patternManager: ActorRef): Receive = {
    case Received(data) =>
      log.debug(s"Input for serial port ${data.decodeString(encoding)}")
      //Feed into the parser that will then send it to the listener
      channel.push(data)

    case posup: PositionUpdate =>
      context.parent ! posup

    case load: LoadPatternRow =>
      patternManager ! load

    case loaded: PatternRowLoaded if sender == self => //from parser
      patternManager ! loaded
    case loaded: PatternRowLoaded => //confirmation by pattern manager
      context.parent ! loaded

    case Closed =>
      throw new RuntimeException("Unexpected close of serial port")
  }
}

private class BrotherPatternManger(operator: ActorRef, encoding: String) extends Actor with ActorLogging {
  override def preStart = {
    // Turn solenoids off
    loadPattern(offPattern, self)
  }
  override def postStop = {
    // Turn solenoids off
    operator ! Write(serializePattern(offPattern))
  }

  override def receive = {
    case LoadPatternRow(pattern) => loadPattern(pattern, sender)
  }

  def loadPattern(pattern: Needle => NeedleAction, by: ActorRef) = {
    val data = serializePattern(pattern)
    val ack = new AckEvent {}
    operator ! Write(data, ack)
    context.setReceiveTimeout(50.millis)
    context become waitPatternSent(ack, pattern, by)
  }
  def waitPatternSent(ack: AckEvent, pattern: Needle => NeedleAction, by: ActorRef): Receive = receive orElse {
    case `ack` =>
      context.setReceiveTimeout(200.millis)
      context become waitPatternConfirmation(pattern, by)
    case CommandFailed(w @ Write(_, `ack`), e) =>
      log.debug(s"Could not set pattern, write command failed: $e. Retrying.")
      operator ! w //retry
    case ReceiveTimeout =>
      log.debug("Could not set pattern, write command failed: Timeout. Retrying.")
      operator ! Write(serializePattern(pattern), ack) //retry
  }
  def waitPatternConfirmation(pattern: Needle => NeedleAction, by: ActorRef): Receive = receive orElse {
    case PatternRowLoaded(p) if Needle.all.forall(n => p(n) == pattern(n)) =>
      by ! PatternRowLoaded(pattern)
      log.debug("Pattern loaded")
      context become receive
    case ReceiveTimeout =>
      log.debug("Did not receive confirmation for pattern loading within the timeout. Retrying.")
      loadPattern(pattern, by) //retry
  }

  def offPattern(needle: Needle) = NeedleToD
  def serializePattern(pattern: Needle => NeedleAction) = {
    val values = Needle.all.map(pattern).map {
      case NeedleToB => "1"
      case NeedleToD => "0"
    }.mkString
    ByteString.apply("$\t>\t" + values + "\n", encoding)
  }
}

/**
 *  Actor that will send MachineEvents to a listener. The actor will crash if there
 *  is a problem with the connection to the machine.
 */
object BrotherConnector {
  def props(port: String, serialManager: ActorRef) = {
    Props(new BrotherConnector(port, serialManager, parser))
  }

  private val carriageWidth = 24
  def parser(input: String): Option[Connector.Event] = {
    input.filterNot(_ == '\r').split('\t').toList match {
      case "@" :: needle :: position :: direction :: carriage :: cpos :: _ =>
        val value = for {
          index <- Try(needle.toInt)
          index2 <- Try(position.toInt)
          n <- Try(Needle.atIndex(index))
          p <- Try(cpos match {
            case "<" => CarriageLeft((carriageWidth + index2).max(0).min(24))
            case ">" => CarriageRight((24 - (index2 - 199)).max(0).min(24))
            case "_" => CarriageOverNeedles(n)
            case other => throw new IllegalArgumentException(s"Unknown needle position: $other")
          })
          d <- Try(direction match {
            case "<-" => Left
            case "->" => Right
            case other => throw new IllegalArgumentException(s"Unknown direction: $other")
          })
          c <- Try(carriage match {
            case "K" => Some(KCarriage)
            case "L" => Some(LCarriage)
            case "G" => Some(GCarriage)
            case " " => None
          })
        } yield PositionUpdate(p, d, c)
        value.
          map(Some(_)).
          recover {
            case e =>
              Logger.debug(s"Failed to parse input from machine: ${e.getMessage}")
              None
          }.
          get

      case "$" :: "<" :: pattern :: _ =>
        (for {
          actions <- Try {
            Needle.all.zip(pattern.map {
              case '1' => NeedleToB
              case '0' => NeedleToD
            }).toMap
          }
        } yield {
          if (actions.size == Needle.needleCount) Some(PatternRowLoaded(actions))
          else {
            Logger.debug(s"Incomplete needle pattern received (${actions.size} instead of ${Needle.needleCount}) actions).")
            None
          }
        }).getOrElse {
          Logger.debug(s"Malformed needle pattern: $pattern")
          None
        }

      case "*" :: msg =>
        Logger.info(s"Machine complains: $msg")
        None

      case malformed if malformed.filter(_.trim.nonEmpty).nonEmpty =>
        Logger.debug(s"Malformed input from machine: $malformed")
        None
      case empty => None
    }
  }
}
