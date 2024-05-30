package kind.logic.js.tables

import kind.logic.js._
import org.scalajs.dom._
import scalatags.JsDom.all._

/** @param source
  *   the data source
  */
case class TableComponent(source: DataSource, numRowsToShow: Int = 40) {

  private var fromRow               = 0
  private var toRow                 = numRowsToShow
  private var sortCol: Option[Sort] = None
  private var userWidthPerCol       = Map.empty[String, Double]

  private def colName(col: String) = col

  private def asRow(row: Row) = {
    tr(row.cells.zipWithIndex.map { (c, i) =>
      val cell = td(c).render
      cell.onclick = _ => {
        source.onCellClick(row, i)
      }
      cell
    }).render
  }

  private def toggleSort(col: String): Unit = {
    sortCol = sortCol match {
      case Some(Sort.Ascending(`col`)) =>
        Some(Sort.Descending(col))
      case _ =>
        Some(Sort.Ascending(col))
    }
    refreshTable()
  }

  private def asHeader(col: String) = {

    val sortSpan = sortCol match {
      case Some(Sort.Ascending(`col`)) =>
        span(style := "margin:10px", "⬆️")
      case Some(Sort.Descending(`col`)) =>
        span(style := "margin:10px", "⬇️")
      case _ => span()
    }

    val handle    = div(cls := "resize-handle").render
    val titleSpan = span(colName(col), sortSpan).render
    val header    = th(titleSpan, handle).render
    titleSpan.onclick = _ => toggleSort(col)

    userWidthPerCol.get(col).foreach { width =>
      header.style.width = s"${width}px"
    }

    var mouseDown  = false
    var startX     = 0.0
    var startWidth = 0.0
    handle.onmousedown = (e1: MouseEvent) => {
      startX = e1.pageX
      startWidth =
        header.offsetWidth - 20 // no idea about the -20  ... there just seems to be a kind of jump when clicked, which this mitigates?
      mouseDown = true

    }
    header.onmousemove = (e: MouseEvent) => {
      if mouseDown then {
        val newWidth = startWidth + e.pageX - startX
        header.style.width = s"${newWidth}px"
        userWidthPerCol = userWidthPerCol.updated(col, newWidth)
      }
    }

    header.onmouseup = _ => mouseDown = false

    header
  }

  private def makeTable = table(
    thead(
      tr(source.columns.map(c => asHeader(c.name)))
    ),
    tbody(
      source.rowsForView(View(fromRow, toRow, sortCol)).map(asRow)
    )
  ).render

  /** The container for the table
    */
  val container = div(cls := "table-container", makeTable).render
  container.onscroll = { _ =>
    val newFromRow = (container.scrollTop / numRowsToShow).toInt
    val newToRow   = newFromRow + numRowsToShow
    if newFromRow != fromRow || newToRow != toRow then {
      fromRow = newFromRow
      toRow = newToRow
      refreshTable()
    }
  }

  def refreshTable() = {
    container.innerHTML = ""
    container.appendChild(makeTable)
  }

}
