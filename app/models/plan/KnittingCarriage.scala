package models.plan

import scala.util.Try
import scalaz._
import Scalaz._
import models._
import utils._

trait KnittingCarriage {
  def apply(direction: Direction)(needles: NeedleStateRow): Validation[String, (NeedleStateRow, Needle => Stitch)]
}

object KnittingCarriage {
  def apply(carriage: Carriage, settings: CarriageSettings, yarnA: Option[Yarn], yarnB: Option[Yarn],
    pattern: Option[NeedleActionRow]) = {
    require(carriage == settings.carriage, "Setting does not match carriage")
    settings match {
      case s: KCarriageSettings => new KKnittingCarriage(s, yarnA, yarnB, pattern)
      case s: LCarriageSettings => new LKnittingCarriage(s, pattern)
      case s: GCarriageSettings => new GKnittingCarriage(s, yarnA, pattern)
    }
  }

  private class KKnittingCarriage(settings: KCarriageSettings,
    yarnA: Option[Yarn], yarnB: Option[Yarn],
    pattern: Option[NeedleActionRow])
    extends KnittingCarriage {

    if (settings.knob == KC2) require(pattern.isDefined, "Need pattern for KC knitting")
    else require(pattern.isEmpty, s"No pattern supported for ${settings.knob}")

    override def apply(direction: Direction)(needles: NeedleStateRow) = Try {
      if (settings.knob == CR) throw new IllegalStateException("Not knitting in CR setting")

      //Basic movement of needles and putting yarn on needles
      val baseState = (needle: Needle) => {
        //TODO part modes
        needles(needle).position match {
          case NeedleA => NeedleState(NeedleA)
          case NeedleB => NeedleState(NeedleB, yarnA)
          case NeedleD => NeedleState(NeedleB, if (settings.mc) yarnB else yarnA)
          case NeedleE if settings.holdingCamLever == HoldingCamN => NeedleState(NeedleB, yarnA)
          case NeedleE => NeedleState(NeedleE, yarnA)
        }
      }
      //Apply the pattern (moves Needles between B and D). 
      val patternState = pattern match {
        case Some(pattern) => (needle: Needle) => {
          val s @ NeedleState(pos, yarn) = baseState(needle)
          if (pos == NeedleB || pos == NeedleD)
            s.copy(position = pattern(needle).toPosition)
          else s
        }
        case None => baseState
      }
      //Knitting performed
      def knitted(n: Needle) = needles(n) match {
        //TODO part modes
        case NeedleState(NeedleB, yarns) if yarns.nonEmpty => PlainStitch(yarns)
        case NeedleState(NeedleD, yarns) if yarns.nonEmpty => PlainStitch(yarns)
        case NeedleState(NeedleE, yarns) if yarns.nonEmpty && settings.holdingCamLever == HoldingCamN =>
          PlainStitch(yarns)
        case NeedleState(NeedleE, _) => NoStitch
        case other => EmptyStitch
      }
      (patternState, knitted _)
    }

    def modifyNeedles(direction: Direction)(before: NeedlePatternRow) = ???
    def knit(row: NeedlePatternRow, next: NeedlePatternRow) = ???
  }

  private class LKnittingCarriage(settings: LCarriageSettings, pattern: Option[NeedleActionRow])
    extends KnittingCarriage {

    def apply(direction: Direction)(needles: NeedleStateRow) = Try {
      if (needles.pattern.all.exists(_ == NeedleE))
        throw new IllegalStateException("LCarriage does not work with needles at E")

      //TODO implement L knitting
      ???
    }
  }

  private class GKnittingCarriage(settings: GCarriageSettings, yarnA: Option[Yarn], pattern: Option[NeedleActionRow])
    extends KnittingCarriage {

    def apply(direction: Direction)(needles: NeedleStateRow) = Try {
      if (needles.pattern.all.exists(_ == NeedleE))
        throw new IllegalStateException("LCarriage does not work with needles at E")
      if (needles.pattern.all.exists(_ == NeedleD))
        throw new IllegalStateException("LCarriage does not work with needles at D")

      ???
    }
  }
}