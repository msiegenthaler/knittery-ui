package knit.plan2

import scalaz._
import knit._

object KnittingPlan {
  import KnittingPlanOps._

  //cannot use FreeC, because type inference for implicits will diverge
  type KnittingPlan[A] = Free[KnittingPlanF, A]

  /** Knits a row using the specified carriage. */
  def knitRow(carriage: Carriage, direction: Direction, pattern: NeedleActionRow = AllNeedlesToB) =
    Free.liftFC(KnitRow(carriage, direction, pattern))

  /** Manual movement of needles. */
  def moveNeedles(bed: Bed, to: NeedlePatternRow) = Free.liftFC(MoveNeedles(bed, to))

  /**
   * Moves the needle into A position and moves the yarns that were on in one needle in the
   * given direction. The needle the yarn is moved to is left in the B position.
   */
  def retireNeedle(bed: Bed, at: Needle, direction: Direction) = Free.liftFC(RetireNeedle(bed, at, direction))

  /** Retire needles with the double decker. */
  def retireWithDouble(bed: Bed, leftmost: Needle, direction: Direction) =
    Free.liftFC(RetireWithDouble(bed, leftmost, direction))

  /** Retire needles with the triple decker. */
  def retireWithTriple(bed: Bed, leftmost: Needle, direction: Direction) =
    Free.liftFC(RetireWithTriple(bed, leftmost, direction))

  def addCarriage(carriage: Carriage, at: LeftRight = Left) = Free.liftFC(AddCarriage(carriage, at))
  def changeKCarriageSettings(settings: KCarriage.Settings, assembly: KCarriage.Assembly)
  = Free.liftFC(ChangeKCarriageSettings(settings, assembly))
  def changeLCarriageSettings(settings: LCarriage.Settings) = Free.liftFC(ChangeLCarriageSettings(settings))
  def changeGCarriageSettings(settings: GCarriage.Settings) = Free.liftFC(ChangeGCarriageSettings(settings))

  def threadYarnK(yarnA: Option[YarnPiece], yarnB: Option[YarnPiece]) = Free.liftFC(ThreadYarnK(yarnA, yarnB))
  def threadYarnG(yarn: Option[YarnPiece]) = Free.liftFC(ThreadYarnG(yarn))

  /**
   * Performs a closed cast on for the needles. The needles are then moved to D position.
   * All other needles are not touched.
   */
  def closedCastOn(bed: Bed, from: Needle, until: Needle, yarn: YarnPiece) =
    Free.liftFC(ClosedCastOn(bed, from, until, yarn))
  def closedCastOff(bed: Bed, withYarn: YarnPiece, filter: Needle => Boolean) =
    Free.liftFC(ClosedCastOff(bed, withYarn, filter))

  /** Moves the yarn from the main to the double bed. Needles affected are moved to B position. */
  def moveToDoubleBed(filter: Needle => Boolean, offset: Int = 0, flip: Option[Needle] = None) =
    Free.liftFC(MoveToDoubleBed(filter, offset, flip))
  /** Transfer the yarn from the double bed to the main bed. Affected needles are moved to B position. */
  def moveToMainBed(filter: Needle => Boolean, offset: Int = 0) = Free.liftFC(MoveToMainBed(filter, offset))

  def hangOnCastOnComb() = Free.liftFC(HangOnCastOnComb)


  object KnittingPlanOps {
    sealed trait KnittingPlanOp[+A]
    type KnittingPlanF[A] = Coyoneda[KnittingPlanOp, A]

    /** Knits a row using a carriage. */
    case class KnitRow(carriage: Carriage, direction: Direction, pattern: NeedleActionRow = AllNeedlesToB)
      extends KnittingPlanOp[Unit]

    /** Manual movement of needles. */
    case class MoveNeedles(bed: Bed, to: NeedlePatternRow) extends KnittingPlanOp[Unit]
    /**
     * Moves the needle into A position and moves the yarns that were on in one needle in the
     * given direction. The needle the yarn is moved to is left in the B position.
     */
    case class RetireNeedle(bed: Bed, at: Needle, direction: Direction) extends KnittingPlanOp[Unit]
    /** Retire needles with the double decker. */
    case class RetireWithDouble(bed: Bed, leftmost: Needle, direction: Direction) extends KnittingPlanOp[Unit]
    /** Retire needles with the triple decker. */
    case class RetireWithTriple(bed: Bed, leftmost: Needle, direction: Direction) extends KnittingPlanOp[Unit]

    case class AddCarriage(carriage: Carriage, at: LeftRight = Left) extends KnittingPlanOp[Unit]
    case class ChangeKCarriageSettings(settings: KCarriage.Settings, assembly: KCarriage.Assembly)
      extends KnittingPlanOp[Unit]
    case class ChangeLCarriageSettings(settings: LCarriage.Settings) extends KnittingPlanOp[Unit]
    case class ChangeGCarriageSettings(settings: GCarriage.Settings) extends KnittingPlanOp[Unit]

    case class ThreadYarnK(yarnA: Option[YarnPiece], yarnB: Option[YarnPiece]) extends KnittingPlanOp[Unit]
    case class ThreadYarnG(yarn: Option[YarnPiece]) extends KnittingPlanOp[Unit]

    /**
     * Performs a closed cast on for the needles. The needles are then moved to D position.
     * All other needles are not touched.
     */
    case class ClosedCastOn(bed: Bed, from: Needle, until: Needle, yarn: YarnPiece) extends KnittingPlanOp[Unit]
    case class ClosedCastOff(bed: Bed, withYarn: YarnPiece, filter: Needle => Boolean) extends KnittingPlanOp[Unit]

    /** Moves the yarn from the main to the double bed. Needles affected are moved to B position. */
    case class MoveToDoubleBed(filter: Needle => Boolean, offset: Int = 0, flip: Option[Needle] = None)
      extends KnittingPlanOp[Unit]
    /** Transfer the yarn from the double bed to the main bed. Affected needles are moved to B position. */
    case class MoveToMainBed(filter: Needle => Boolean, offset: Int = 0) extends KnittingPlanOp[Unit]

    case object HangOnCastOnComb extends KnittingPlanOp[Unit]
  }
}
