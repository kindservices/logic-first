package kind.logic

import org.scalajs.dom.html.Div
import org.scalajs.dom.{MouseEvent, window}
import org.scalajs.dom.html.{Div, Input}
import scalatags.JsDom.all.*
import org.scalajs.dom.Node
import scala.scalajs.js.JSON

import java.time.format.DateTimeFormatter
import concurrent.duration.*
import java.time.{ZoneId, ZonedDateTime}

import upickle.default.{ReadWriter => RW, macroRW}
import upickle.default.*
import ujson.Value

package object js {
  type Json = ujson.Value

  /** This layout is exported in main.js for us to reference here */
  def myLayout = scala.scalajs.js.Dynamic.global.window.myLayout

  extension (container: scala.scalajs.js.Dynamic) {
    def placeholder(name: String, state: scala.scalajs.js.Dynamic) = {
      container.title = name
      container
        .getElement()
        // .html(div(h2(cls := "subtitle", s"${name}!"), div(JSON.stringify(state))).render)
        .html(div(h2(cls := "subtitle", s"${name}!"), div(state.toString)).render)
    }

    def replace(node: Node) = container.getElement().html(node)
  }

  extension (jason: String) {
    def asUJson: Value = ujson.read(jason)
    def asJSON         = JSON.parse(jason)
  }

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

  // for each stack in a test call frame, there is an input and an output
  type StackElement = (Json, Json)

  /** The output of running a test scenario
    */
  case class TestResult(callstack: Seq[StackElement] = Nil, result: Json = ujson.Null)
      derives ReadWriter {
    def asJson: String = write(this)
  }

  object TestResult {
    def fromJson(jason: Json): TestResult = read[TestResult](jason)
  }

}
