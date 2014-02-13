package models.planners

import scalaz._
import Scalaz._
import models._
import models.plan._

object Cast {
  def onClosed(from: Needle, until: Needle, withYarn: Yarn): PlannerM[YarnFlow] =
    onClosed(from, until, YarnPiece(withYarn))
  def onClosed(from: Needle, until: Needle, withYarn: YarnPiece): PlannerM[YarnFlow] = {
    Planner.step(ClosedCastOn(from, until, withYarn)) >>
      Basics.yarnAttachment(withYarn).map(_.get.yarn)
  }

  def onClosedRound(from: Needle, until: Needle, withYarn: Yarn) = for {
    _ <- Planner.noop
    until2 <- Planner.precondidtions { _ =>
      require(from < until, "from > until")
      val u2 = until.index + (until - from)
      require(u2 < Needle.count, "Needle bed not wide enough")
      Needle.atIndex(u2)
    }
    //TODO basically we'd need to knit with contrast yarn first in order to
    // be able to move the yarn properly..
    yarn <- onClosed(from, until2, withYarn)
    _ <- Basics.knitRowWithK(KCarriage.Settings(), Some(yarn.start))
    _ <- MoveToDoubleBed(n => n >= from && n <= until2, 0, Some(until))
  } yield yarn

  def offClosed(withYarn: YarnPiece, filter: Needle => Boolean = _ => true): Planner = for {
    needleState <- Planner.state(_.needles)
    _ <- ClosedCastOff(withYarn, n => filter(n) && needleState(n).yarn.nonEmpty)
  } yield ()
}