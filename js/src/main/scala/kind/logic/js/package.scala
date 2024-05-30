package kind.logic

import ujson.Value

import scala.scalajs.js.JSON

package object js {
  type Json = ujson.Value

  extension (jason: String) {
    def asUJson: Value = ujson.read(jason)
    def asJSON         = JSON.parse(jason)
  }

  // for each stack in a test call frame, there is an input and an output
  type StackElement = (Json, Json)
}
