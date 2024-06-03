package kind.logic.json

import upickle.default.*

enum Filter derives ReadWriter:
  case Pass
  case Not(other: Filter)
  case Contains(field: String, contains: String) // this field contains the given string
  case ContainsAny(contains: String) // this field contains the given string
  case Eq(field: String, value: Json)
  case Match(value: Json)
  case LT(field: String, value: Double)
  case LTE(field: String, value: Double)
  case GT(field: String, value: Double)
  case GTE(field: String, value: Double)
  case And(left: Filter, right: Filter)
  case Or(left: Filter, right: Filter)

  def not                = Not(this)
  def and(other: Filter) = And(this, other)
  def or(other: Filter)  = Or(this, other)

  private def compare(data: Json, field: String)(f: Double => Boolean) = {
    data.objOpt.fold(false) { obj =>
      obj.get(field).exists(_.numOpt.exists(f))
    }
  }

  /** Apply this filter to the given data
    * @param data
    *   the input data
    * @return
    *   whether the filter applies
    */
  def test(data: Json): Boolean = this match {
    case Pass            => true
    case Not(filter)     => !filter.test(data)
    case Match(expected) => data == expected
    case ContainsAny(text) => data.render(0).contains(text)
    case Contains(field, expected) =>
      data.objOpt.fold(false) { obj =>
        obj.get(field).exists(_.render(0).contains(expected))
      }
    case Eq(field, expected) =>
      data.objOpt.fold(false) { obj =>
        obj.get(field).contains(expected)
      }
    case LT(field, expected)  => compare(data, field)(_ < expected)
    case LTE(field, expected) => compare(data, field)(_ <= expected)
    case GT(field, expected)  => compare(data, field)(_ > expected)
    case GTE(field, expected) => compare(data, field)(_ >= expected)
    case And(left, right)     => left.test(data) && right.test(data)
    case Or(left, right)      => left.test(data) || right.test(data)
  }
