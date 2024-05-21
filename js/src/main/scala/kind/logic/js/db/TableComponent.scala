package kind.logic.js.db

import zio.*
import kind.logic.js.*
import kind.logic.json.*
import scalatags.JsDom.all.*
import org.scalajs.dom.*

/** @param identifier
  *   an ID for this database (so we can send relevant notifications)
  * @param dataByIdRef
  *   the data in the database, stored in a Ref
  * @param columns
  *   the optional DB columns to show
  */
class TableComponent(
    identifier: String,
    dataByIdRef: Ref[Map[String, Json]],
    columns: Seq[String] = Nil
) {

  private def colName(col: String) = col

  private def asRow(key: String, data: Json) = {
    val row = tr(
      td(key) +: columns.map(c => tr(data.at(c).map(_.render()).getOrElse(" - ")))
    ).render
    row
  }

  def asHeader(col: String) = {
    val sortUp   = span(style := "margin:10px", "⬆️").render
    val sortDown = span(style := "margin:10px", "⬇️").render
    val header   = th(span(colName(col), sortUp, sortDown)).render

    header
  }

  private def asTable(data: Map[String, Json]) = {
    val rows = data.toSeq.sortBy(_._1).map { case (key, data) =>
      asRow(key, data)
    }

    table(
      thead(
        ("Key" +: columns).map(asHeader)
      )
    )(
      tbody(
        rows
      )
    ).render
  }

  private lazy val container = div().render
  def update(body: HTMLElement) = {
    HtmlUtils.replace(container, body)
  }

  def element = dataByIdRef.get.map { data =>
    update(asTable(data))
    container
  }
}

object TableComponent {

  def apply(dbRef: Ref[Map[String, Json]]) = {
    dbRef.get.map { db =>
      val cols = db.values.flatMap(_.paths).toSeq.distinct.sorted
      new TableComponent("", dbRef, cols)
    }
  }
}
