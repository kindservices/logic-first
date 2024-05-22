package kind.logic.js.tables

enum Sort:
  case Ascending(col: String)
  case Descending(col: String)

  def ascending: Boolean = this match {
    case Ascending(_)  => true
    case Descending(_) => false
  }

  def descending: Boolean = !ascending

  def column: String = this match {
    case Ascending(col)  => col
    case Descending(col) => col
  }
