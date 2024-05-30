package kind.logic.js

import scalatags.JsDom.all._
import ujson.Value
import upickle.default._
import upickle.default.{ReadWriter => RW}

/** The output of running a test scenario
  */
case class TestResult(callstack: Seq[StackElement] = Nil, result: Json = ujson.Null)
    derives ReadWriter {
  def asJson: String = write(this)
}

object TestResult {
  def fromJson(jason: Json): TestResult = read[TestResult](jason)
}
