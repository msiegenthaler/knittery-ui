package models

import java.awt.Color
import utils.ConsoleColors

case class Yarn(name: String, color: Color) {
  def consoleColor = ConsoleColors.closest(color)
  override def toString = name
}

sealed trait YarnFlow {
  def yarn: Yarn
  def next(distance: Int) = YarnPoint(yarn, YarnFlow.this, distance)
  def nexts(distance: Int): Stream[YarnPoint] = {
    val n = next(distance)
    n #:: n.nexts(distance)
  }
  def previous: Stream[YarnFlow]
  /** This element and all the previous elements. */
  def stream = this #:: previous
  def start: YarnStart
}
case class YarnPoint(yarn: Yarn, prev: YarnFlow, distance: Int) extends YarnFlow {
  override def previous = prev.stream
  override def start = prev.start
}
class YarnStart(val yarn: Yarn) extends YarnFlow {
  override def previous = Stream.empty
  override def start = this
  //equals must be for object identity
  override def toString = s"YarnStart($yarn)"
}
object YarnStart {
  def apply(yarn: Yarn) = new YarnStart(yarn)
}