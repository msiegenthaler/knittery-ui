package models

import scalaz._
import Scalaz._

package object plan {
  type NeedleStateRow = Needle => NeedleState
  implicit class RichNeedleStateRow(val row: NeedleStateRow) extends AnyVal {
    def pattern = row.andThen(_.position)
    def yarn = row.andThen(_.yarn)
  }

  type Planner = PlannerM[Unit]
  implicit def planMonoid = Plan.planMonoid
  implicit def plannerMonad = PlannerM.plannerMonad
  implicit def plannerMonoid = Planner.plannerMonoid
  implicit def stepToPlanner(s: Step) = Planner.step(s)
  implicit def stepToPlannerBindOps(s: Step) = Planner.stepToPlannerBindOps(s)
  implicit def stepToPlannerFunctorOps(s: Step) = Planner.stepToPlannerFunctorOps(s)
}