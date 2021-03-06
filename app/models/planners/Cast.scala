package models.planners

import scalaz._
import Scalaz._
import models._
import models.plan._
import models.KCarriage.{DoubleBedCarriage, TensionDial}

object Cast {
  def onOpen(from: Needle, until: Needle, withYarn: YarnPiece): PlannerM[YarnPiece] = for {
    _ <- Planner.precondidtions(_ => require(from < until))
    needles = Needle.interval(from, until)
    castOnNeedles = needles.filter(_.index % 2 == 0)
    _ <- MoveNeedles(MainBed, n => if (castOnNeedles.contains(n)) NeedleB else NeedleA)
    _ <- Basics.knitRowWithK(yarnA = Some(withYarn))
    _ <- HangOnCastOnComb()
    _ <- MoveNeedles(MainBed, n => if (needles.contains(n)) NeedleB else NeedleA)
    _ <- Basics.knitRowWithK(yarnA = Some(withYarn))
  } yield withYarn

  def onClosed(bed: Bed, from: Needle, until: Needle, withYarn: Yarn): PlannerM[YarnPiece] =
    onClosed(bed, from, until, YarnPiece(withYarn))
  def onClosed(bed: Bed, from: Needle, until: Needle, withYarn: YarnPiece): PlannerM[YarnPiece] = {
    Planner.step(ClosedCastOn(bed, from, until, withYarn)).map(_ => withYarn)
  }

  def onClosedRound(from: Needle, until: Needle, yarn: YarnPiece) = for {
    _ <- Planner.noop
    until2 <- Planner.precondidtions { _ =>
      require(from <= until, s"Cannot perform closed round cast on from right to left ($from -> $until)")
      val u2 = until.index + (until distanceTo from) - 1
      require(u2 < Needle.count, "Needle bed not wide enough")
      Needle.atIndex(u2)
    }
    contrastYarn = YarnPiece(Yarn.contrastYarn)
    _ <- onOpen(from, until2, contrastYarn)
    _ <- (1 to 25).toVector.traverse(_ => Basics.knitRowWithK(yarnA = Some(contrastYarn)))
    _ <- onClosed(MainBed, from, until2, yarn)
    _ <- Basics.needCarriage(KCarriage, Right)
    _ <- Basics.knitRowWithK(yarnA = Some(yarn))
    _ <- MoveToDoubleBed(n => n >= until && n <= until2, -1, Some(until))
    _ <- MoveNeedles(MainBed, (n: Needle) => if (n >= from && n < until) NeedleB else NeedleA)
  } yield yarn

  def onDoubleBed(from: Needle, until: Needle, withYarn: YarnPiece): Planner = for {
    _ <- MoveNeedles(MainBed, (n: Needle) => if (n >= from && n < until) NeedleB else NeedleA)
    _ <- MoveNeedles(DoubleBed, (n: Needle) => if (n >= from && n < until - 1) NeedleB else NeedleA)
    _ <- Basics.needCarriage(KCarriage, Right)
    _ <- Basics.knitRowWithK(assembly = DoubleBedCarriage(), yarnA = Some(withYarn))
    _ <- HangOnCastOnComb()
    _ <- Basics.knitRowWithK(settings = KCarriage.Settings(tension = TensionDial(1, 0), partLeft = true),
      assembly = DoubleBedCarriage(tension = TensionDial(1, 0), partRight = true), yarnA = Some(withYarn))
    _ <- Basics.knitRowWithK(settings = KCarriage.Settings(tension = TensionDial(1, 0), partLeft = true),
      assembly = DoubleBedCarriage(tension = TensionDial(1, 0), partRight = true), yarnA = Some(withYarn))
    _ <- Basics.knitRowWithK(settings = KCarriage.Settings(tension = TensionDial(2, 0)),
      assembly = DoubleBedCarriage(tension = TensionDial(2, 0)), yarnA = Some(withYarn))
    _ <- Basics.knitRowWithK(settings = KCarriage.Settings(tension = TensionDial(2, 0)),
      assembly = DoubleBedCarriage(tension = TensionDial(2, 0)), yarnA = Some(withYarn))
  } yield ()

  def offClosed(bed: Bed, withYarn: YarnPiece, filter: Needle => Boolean = _ => true): Planner = for {
    needleState <- Planner.state(_.needles(bed))
    _ <- ClosedCastOff(bed, withYarn, n => filter(n) && needleState(n).yarn.nonEmpty)
  } yield ()

  def offDoubleBed(withYarn: YarnPiece, filter: Needle => Boolean = _ => true): Planner = for {
    _ <- MoveToMainBed(filter)
    _ <- MoveNeedles(DoubleBed, _ => NeedleA)
    _ <- Basics.knitRowWithK(yarnA = Some(withYarn))
    _ <- offClosed(MainBed, withYarn, filter)
  } yield ()
}