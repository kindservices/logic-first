package kind.logic.json

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ujson.*
import kind.logic.*

import scala.util.Success

class FilterTest extends AnyWordSpec with Matchers {

  import Filter.*
  "Filter" should {
    List(
      Contains("foo", "bar"),
      LTE("foo", 123),
      GTE("foo", 456),
      Eq("foo", "bar".asJsonString),
      Eq("foo", "bar".asJsonString).and(Eq("foo", "buzz".asJsonString))
    ).foreach { filter =>
      s"be able to serialise $filter to/from json: ${filter.asUJson.render(0)}" in {
        filter.asUJson.as[Filter] shouldBe Success(filter)
      }
    }

  }
}
