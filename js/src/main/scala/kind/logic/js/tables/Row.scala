package kind.logic.js.tables

import org.scalajs.dom.Node
import scalatags.JsDom.all._

final case class Row(cells: Seq[Node])
object Row {
  def apply(first: Node, theRest: Node*): Row = new Row(first +: theRest.toSeq)
  def forContent(content: Seq[String]): Row = new Row(content.map { c =>
    span(c).render
  })
}
