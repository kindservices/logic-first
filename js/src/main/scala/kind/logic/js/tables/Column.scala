package kind.logic.js.tables

final case class Column(id: String, name: String)
object Column {
  def apply(name: String): Column = new Column(name, name)
}
