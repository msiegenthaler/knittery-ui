import scalaz._
import Scalaz._
import scala.util.Try

package object utils {
  type Matrix[A] = IndexedSeq[IndexedSeq[A]]

  implicit class RichMatrix[A](val matrix: Matrix[A]) extends AnyVal {
    import RichMatrix._
    def width = matrix.headOption.map(_.size).getOrElse(0)
    def height = matrix.size
    def rows = matrix
    def columns = transpose
    def transpose: IndexedSeq[IndexedSeq[A]] = matrix match {
      case m: TransposedMatrix[A] => m.matrix //don't make a chain of wrapping
      case m => new TransposedMatrix(m)
    }
    def matrixMap[B](f: A => B): Matrix[B] = matrix.map(_.map(f))
    def reverseBoth = matrix.columns.reverse.transpose.reverse
    def validate() = {
      if (matrix.nonEmpty) matrix.foreach(r => require(r.size == width, s"row widths are non-equal: ${r.size} != $width"))
    }
  }
  private object RichMatrix {
    private class TransposedMatrix[A](val matrix: Matrix[A]) extends IndexedSeq[IndexedSeq[A]] {
      override def apply(index: Int): IndexedSeq[A] = matrix.map(_(index))
      override def length = matrix.width
    }
  }

  implicit class RichTry[A](val t: Try[A]) {
    def toSuccess: Validation[String, A] = {
      t.map(_.success).recover {
        case e: Exception => e.getMessage.fail
      }.get

    }
  }
  implicit def tryToValidation[A](t: Try[A]): Validation[String, A] = t.toSuccess
}