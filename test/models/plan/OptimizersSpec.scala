package models.plan

import org.specs2.mutable.Specification
import org.specs2._
import models._
import models.planners._

class OptimizersSpec extends Specification {
  trait plans extends Yarns {
    val allNeedles = (n: Needle) => true

    val simpleLines = Plan(List(
      ClosedCastOn(Needle.atIndex(1), Needle.atIndex(40), red),
      AddCarriage(KCarriage, Left),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ThreadYarn(Some(red), None),
      KnitRow(KCarriage, ToRight),
      KnitRow(KCarriage, ToLeft),
      KnitRow(KCarriage, ToRight),
      KnitRow(KCarriage, ToLeft),
      KnitRow(KCarriage, ToRight),
      KnitRow(KCarriage, ToLeft),
      ClosedCastOff(red, allNeedles)))

    val simpleLinesWithUnknittedSettings = Plan(List(
      ClosedCastOn(Needle.atIndex(1), Needle.atIndex(40), red),
      AddCarriage(KCarriage, Left),
      ChangeCarriageSettings(LCarriageSettings()),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ThreadYarn(Some(red), None),
      KnitRow(KCarriage, ToRight),
      KnitRow(KCarriage, ToLeft),
      KnitRow(KCarriage, ToRight),
      KnitRow(KCarriage, ToLeft),
      KnitRow(KCarriage, ToRight),
      KnitRow(KCarriage, ToLeft),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ClosedCastOff(red, allNeedles)))

    val simpleLinesWithDuplicateSettings = Plan(List(
      ClosedCastOn(Needle.atIndex(1), Needle.atIndex(40), red),
      AddCarriage(KCarriage, Left),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ThreadYarn(Some(red), None),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToRight),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToLeft),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToRight),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToLeft),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToRight),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToLeft),
      ClosedCastOff(red, allNeedles)))

    val simpleLinesWitUselessSettings = Plan(List(
      ClosedCastOn(Needle.atIndex(1), Needle.atIndex(40), red),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      AddCarriage(KCarriage, Left),
      ChangeCarriageSettings(LCarriageSettings()),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ThreadYarn(Some(red), None),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToRight),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToLeft),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToRight),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToLeft),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToRight),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      KnitRow(KCarriage, ToLeft),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ClosedCastOff(red, allNeedles)))

    def evenOddPattern(n: Needle) = if (n.index % 2 == 0 || n.index >= 1 || n.index <= 40) NeedleToB else NeedleToD
    def oddEvenPattern(n: Needle) = if (n.index % 2 == 1 || n.index >= 1 || n.index <= 40) NeedleToB else NeedleToD

    val patternLines = Plan(List(
      ClosedCastOn(Needle.atIndex(1), Needle.atIndex(40), red),
      AddCarriage(KCarriage, Left),
      ChangeCarriageSettings(KCarriageSettings(KC2)),
      ThreadYarn(Some(red), None),
      KnitRow(KCarriage, ToRight, evenOddPattern),
      ChangeCarriageSettings(KCarriageSettings(KC2, mc = true)),
      KnitRow(KCarriage, ToLeft, oddEvenPattern),
      KnitRow(KCarriage, ToRight, evenOddPattern),
      KnitRow(KCarriage, ToLeft, oddEvenPattern),
      KnitRow(KCarriage, ToRight, evenOddPattern),
      KnitRow(KCarriage, ToLeft, oddEvenPattern),
      KnitRow(KCarriage, ToRight, evenOddPattern),
      KnitRow(KCarriage, ToLeft),
      ClosedCastOff(red, allNeedles)))

    val patternLinesWithManualNeedleSettings = {
      def line(n: Needle) = if (n.index >= 1 && n.index <= 40) NeedleB else NeedleA
      Plan(List(
        ClosedCastOn(Needle.atIndex(1), Needle.atIndex(40), red),
        AddCarriage(KCarriage, Left),
        ChangeCarriageSettings(KCarriageSettings(KC2)),
        ThreadYarn(Some(red), None),
        KnitRow(KCarriage, ToRight, evenOddPattern),
        ChangeCarriageSettings(KCarriageSettings(KC2, mc = true)),
        MoveNeedles(line, oddEvenPattern),
        KnitRow(KCarriage, ToLeft),
        MoveNeedles(line, oddEvenPattern),
        KnitRow(KCarriage, ToRight),
        MoveNeedles(line, oddEvenPattern),
        KnitRow(KCarriage, ToLeft),
        MoveNeedles(line, oddEvenPattern),
        KnitRow(KCarriage, ToRight),
        MoveNeedles(line, oddEvenPattern),
        KnitRow(KCarriage, ToLeft),
        MoveNeedles(line, oddEvenPattern),
        KnitRow(KCarriage, ToRight),
        MoveNeedles(line, oddEvenPattern),
        KnitRow(KCarriage, ToLeft),
        ClosedCastOff(red, allNeedles)))
    }

    val plans = simpleLines ::
      simpleLinesWithUnknittedSettings ::
      simpleLinesWithDuplicateSettings ::
      patternLines ::
      patternLinesWithManualNeedleSettings ::
      Nil
  }

  "optimizers" should {
    def sameOutput(p: Plan) = {
      val unopt = p.run
      val opt = Plan(Optimizers.all(p.steps)).run
      ("Plan fails: " + unopt) <==> (unopt.isSuccess must beTrue)
      opt.isSuccess must beTrue
      unopt.map(_.output) must_== opt.map(_.output)
    }

    "not change result" in new plans {
      forall(plans)(sameOutput)
    }
  }

  "unknitted settings optimizer" should {
    "not change already optimal" in new plans {
      UnknittedSettingsOptimizer(simpleLines.steps) must_== simpleLines.steps
    }
    "remove unknitted change settings" in new plans {
      UnknittedSettingsOptimizer(simpleLinesWithUnknittedSettings.steps) must
        containTheSameElementsAs(simpleLines.steps)
    }
  }
  "duplicate settings optimizer" should {
    "not change already optimal" in new plans {
      DuplicateSettingsOptimizer(simpleLines.steps) must_== simpleLines.steps
    }
    "remove duplicate change settings" in new plans {
      DuplicateSettingsOptimizer(simpleLinesWithDuplicateSettings.steps) must
        containTheSameElementsAs(simpleLines.steps)
    }
  }
  "settings optimizers" should {
    "not change already optimal" in new plans {
      DuplicateSettingsOptimizer(simpleLines.steps) must_== simpleLines.steps
    }
    "remove useless change settings" in new plans {
      DuplicateSettingsOptimizer(
        UnknittedSettingsOptimizer(simpleLinesWithDuplicateSettings.steps)) must
        containTheSameElementsAs(simpleLines.steps)
    }
  }

  "pattern knitting optimizer" should {
    "not change already optimal" in new plans {
      OptimizePatternKnitting(patternLines.steps) must_== patternLines.steps
    }
    "prevent manual needle movement" in new plans {
      OptimizePatternKnitting(patternLinesWithManualNeedleSettings.steps) must
        containTheSameElementsAs(patternLines.steps)
    }
  }
}