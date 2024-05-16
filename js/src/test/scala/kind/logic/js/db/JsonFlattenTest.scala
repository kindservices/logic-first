package kind.logic.js.db

import kind.logic.js.db.paths
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ujson.*

class JsonFlattenTest extends AnyWordSpec with Matchers {
  "JsonFlatten" should {
    "flatten json" in {
      val input = read("""{ 
        "hello" : "World",
        "array" : [
          { "arrayObj" : true},
          2,
          { }
        ],
        "nested" : {
          "ok" : true
        }
      }""")

      val expected = List(
        "array.0.arrayObj",
        "" +
          "array.1",
        "hello",
        "nested.ok"
      )
      input.paths should contain theSameElementsAs (expected)
    }

  }
}
