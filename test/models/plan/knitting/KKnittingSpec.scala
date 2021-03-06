package models.plan.knitting

import scalaz._
import Scalaz._
import org.specs2.mutable.Specification
import models._
import models.plan._

class KKnittingSpec extends Specification with KnittingStateMatchers {
  private trait plain extends Yarns with StateSupport {
    import KCarriage._
    val redPiece = YarnPiece(red)
    def plain = State(yarnA = Some(redPiece))
    def plainH = State(yarnA = Some(redPiece), settings = Settings(holdingCamLever = HoldingCamH))
    def state(pos: Needle => NeedlePosition) = stateWithYarn(pos, redPiece)
  }
  private trait part extends Yarns with StateSupport {
    import KCarriage._
    val redPiece = YarnPiece(red)
    def part = State(yarnA = Some(redPiece), settings = Settings(partLeft = true, partRight = true))
    def partH = State(yarnA = Some(redPiece), settings = Settings(partLeft = true, partRight = true, holdingCamLever = HoldingCamH))
    def state(pos: Needle => NeedlePosition) = stateWithYarn(pos, redPiece)
  }
  private trait mc extends Yarns with StateSupport {
    import KCarriage._
    val redPiece = YarnPiece(red)
    val greenPiece = YarnPiece(green)
    def mc = State(yarnA = Some(redPiece), yarnB = Some(greenPiece), settings = Settings(mc = true))
    def mcH = State(yarnA = Some(redPiece), yarnB = Some(greenPiece), settings = Settings(mc = true, holdingCamLever = HoldingCamH))
    def state(pos: Needle => NeedlePosition) = stateWithYarn(pos, redPiece)
  }

  "plain single bed knitting" should {
    "knit nothing with all needles to A" in new plain {
      val knit = new KKnitting(plain, state(_ => NeedleA), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(EmptyStitch)
      end.needles(MainBed) must beAtPosition(NeedleA)
      end.needles(MainBed) must carryYarn()
    }
    "knit plain red stitches with needles to B" in new plain {
      val knit = new KKnitting(plain, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to D" in new plain {
      val knit = new KKnitting(plain, state(_ => NeedleD), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to E" in new plain {
      val knit = new KKnitting(plain, state(_ => NeedleE), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
  }
  "plain single bed knitting with holdingCamLever=H" should {
    "knit nothing with all needles to A" in new plain {
      val knit = new KKnitting(plainH, state(_ => NeedleA), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(EmptyStitch)
      end.needles(MainBed) must beAtPosition(NeedleA)
      end.needles(MainBed) must carryYarn()
    }
    "knit plain red stitches with needles to B" in new plain {
      val knit = new KKnitting(plainH, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to D" in new plain {
      val knit = new KKnitting(plainH, state(_ => NeedleD), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit nothing with needles to E" in new plain {
      val knit = new KKnitting(plainH, state(_ => NeedleE), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(NoStitch)
      end.needles(MainBed) must beAtPosition(NeedleE)
      end.needles(MainBed) must carryYarn(redPiece)
    }
  }
  "plain single bed knitting with D-pattern" should {
    "knit nothing with all needles to A" in new plain {
      val knit = new KKnitting(plain, state(_ => NeedleA), ToRight)
      val end = knit(AllNeedlesToD).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(EmptyStitch)
      end.needles(MainBed) must beAtPosition(NeedleA)
      end.needles(MainBed) must carryYarn()
    }
    "knit plain red stitches with needles to B and move needles to D" in new plain {
      val knit = new KKnitting(plain, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToD).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleD)
      end.needles(MainBed) must carryYarn(redPiece)
    }
  }

  "part single bed knitting" should {
    "knit nothing with all needles to A" in new part {
      val knit = new KKnitting(part, state(_ => NeedleA), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(EmptyStitch)
      end.needles(MainBed) must beAtPosition(NeedleA)
      end.needles(MainBed) must carryYarn()
    }
    "knit nothing with all needles to B" in new part {
      val knit = new KKnitting(part, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(NoStitch)
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to D" in new part {
      val knit = new KKnitting(part, state(_ => NeedleD), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to E" in new part {
      val knit = new KKnitting(part, state(_ => NeedleE), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
  }
  "part single bed knitting with holdingCamLever=H " should {
    "knit nothing with all needles to B" in new part {
      val knit = new KKnitting(partH, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(NoStitch)
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to D" in new part {
      val knit = new KKnitting(partH, state(_ => NeedleD), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit nothing with needles to E" in new part {
      val knit = new KKnitting(partH, state(_ => NeedleE), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(NoStitch)
      end.needles(MainBed) must beAtPosition(NeedleE)
      end.needles(MainBed) must carryYarn(redPiece)
    }

  }
  "part single bed knitting with pattern D" should {
    "knit nothing with all needles to B and move needles to D" in new part {
      val knit = new KKnitting(part, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToD).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(NoStitch)
      end.needles(MainBed) must beAtPosition(NeedleD)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to D and move needles to D" in new part {
      val knit = new KKnitting(part, state(_ => NeedleD), ToRight)
      val end = knit(AllNeedlesToD).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleD)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red stitches with needles to E and move needles to D" in new part {
      val knit = new KKnitting(part, state(_ => NeedleE), ToRight)
      val end = knit(AllNeedlesToD).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleD)
      end.needles(MainBed) must carryYarn(redPiece)
    }
  }

  "mc single bed knitting" should {
    "knit nothing with all needles to A" in new mc {
      val knit = new KKnitting(mc, state(_ => NeedleA), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(EmptyStitch)
      end.needles(MainBed) must beAtPosition(NeedleA)
      end.needles(MainBed) must carryYarn()
    }
    "knit plain red and put red on needle with needles at B" in new mc {
      val knit = new KKnitting(mc, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red and put green on needle with needles at D" in new mc {
      val knit = new KKnitting(mc, state(_ => NeedleD), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(greenPiece)
    }
    "knit plain red and put green on needle with needles at E" in new mc {
      val knit = new KKnitting(mc, state(_ => NeedleE), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(greenPiece)
    }
  }
  "mc single bed knitting with holdingCamLever=H " should {
    "knit plain red and put red on needle with needles at B" in new mc {
      val knit = new KKnitting(mcH, state(_ => NeedleB), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(redPiece)
    }
    "knit plain red and put green on needle with needles at D" in new mc {
      val knit = new KKnitting(mcH, state(_ => NeedleD), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(PlainStitch(red))
      end.needles(MainBed) must beAtPosition(NeedleB)
      end.needles(MainBed) must carryYarn(greenPiece)
    }
    "knit nothing with needles at E" in new mc {
      val knit = new KKnitting(mcH, state(_ => NeedleE), ToRight)
      val end = knit(AllNeedlesToB).check()
      end.output.mainBed.rows.size must_== 1
      end.output.mainBed.rows(0) must beAllStitch(NoStitch)
      end.needles(MainBed) must beAtPosition(NeedleE)
      end.needles(MainBed) must carryYarn(redPiece)
    }
  }
}