package kind.logic.js.tables

import zio.*
import scalatags.JsDom.all.*
import kind.logic.*

/** A generic representation of the data behind a table.
  */
trait DataSource {

  def columns: Seq[Column]

  def onCellClick(row: Row, cellIndex: Int): Unit = {}

  final def withHandler(handler: DataSource.OnCellClick): DataSource = {
    new DataSource.Delegate(this, handler)
  }

  def rowsForView(view: View): Seq[Row]

}

object DataSource {

  type OnCellClick = (Row, Int) => Unit

  final case class Delegate(underlying: DataSource, handler: OnCellClick) extends DataSource {

    override def columns = underlying.columns

    override def onCellClick(row: Row, cellIndex: Int): Unit = handler(row, cellIndex)

    override def rowsForView(view: View) = underlying.rowsForView(view)

  }

  case class SourceForRef[A](
      ref: Ref[A],
      override val columns: Seq[Column],
      asRow: (A, View) => Seq[Row]
  ) extends DataSource {
    def rowsForView(view: View): Seq[Row] = {
      asRow(ref.get.execOrThrow(), view)

    }
  }

  case class Fixed(rows: Seq[Row], override val columns: Seq[Column]) extends DataSource {
    def rowsForView(range: View): Seq[Row] = {
      import range.*

      val sorted = sortCol match {
        case Some(Sort.Ascending(col)) =>
          rows.sortBy { row =>
            row.cells(columns.indexWhere(_.id == col)).textContent
          }
        case Some(Sort.Descending(col)) =>
          rows.sortBy { row =>
            row.cells(columns.indexWhere(_.id == col)).textContent
          }.reverse
        case None => rows
      }

      sorted.drop(fromRow).take(toRow - fromRow)
    }
  }

  def forRef[A](ref: Ref[A], columns: Seq[String])(asRow: (A, View) => Seq[Row]) = {
    SourceForRef(ref, columns.map(Column.apply), asRow)
  }

  def fixed(rows: Seq[Map[String, String]]): Fixed = {
    val columns = rows.flatMap(_.keySet).distinct.sorted
    val rowList = rows.map { rowMap =>
      val cells = columns.map { col =>
        rowMap.getOrElse(col, "")
      }
      Row.forContent(cells)
    }
    Fixed(rowList, columns.map(Column.apply))
  }
}
