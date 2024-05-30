package kind.logic.js

import scalatags.JsDom.all._
import ujson.Value
import upickle.default._
import upickle.default.{ReadWriter => RW}

/** Represents a named scenario
  *
  * @param name
  *   the friendly name of this scenario
  * @param description
  *   the description
  * @param input
  *   the input into this scenario
  * @param lastResult
  *   the result from the most-recent run of this scenario
  */
case class TestScenario(
    name: String,
    description: String,
    input: Json = ujson.Null,
    lastResult: Option[TestResult] = None
) derives ReadWriter {
  def asJson: Value  = writeJs(this)
  def inputAs[A: RW] = read[A](input)
}

object TestScenario {
  def fromJson(jason: Json): TestScenario                 = read[TestScenario](jason)
  def mapFromJson(jason: Json): Map[String, TestScenario] = read[Map[String, TestScenario]](jason)

}
