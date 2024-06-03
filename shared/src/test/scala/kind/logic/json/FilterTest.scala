package kind.logic.json

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ujson.*
import kind.logic.*

import scala.util.Success

class FilterTest extends AnyWordSpec with Matchers {

  import Filter.*

  "Filter.Contains" should {
    "filter" in {
      Filter.Contains("name", "foo").test(Map("another" -> "foo").asUJson) shouldBe false
      Filter.Contains("name", "foo").test(Map("name" -> "foo").asUJson) shouldBe true
      Filter.Contains("name", "foo").test(Map("name" -> "toe-foo").asUJson) shouldBe true
      Filter.Contains("name", "foo").test(Map("name" -> "bar").asUJson) shouldBe false
    }
  }

  "Filter.Eq" should {
    "filter" in {
      Filter.Eq("name", "foo").test(Map("another" -> "foo").asUJson) shouldBe false
      Filter.Eq("name", "foo").test(Map("name" -> "foo").asUJson) shouldBe true
      Filter.Eq("name", "foo").test(Map("name" -> "toe-foo").asUJson) shouldBe false
      Filter.Eq("name", "foo").test(Map("name" -> "bar").asUJson) shouldBe false
    }
  }

  "Filter.LT" should {
    "filter" in {
      Filter.LT("name", 123).test(Map("another" -> 10).asUJson) shouldBe false
      Filter.LT("name", 123).test(Map("name" -> 124).asUJson) shouldBe false
      Filter.LT("name", 123).test(Map("name" -> 122).asUJson) shouldBe true
      Filter.LT("name", 123).test(Map("name" -> 123).asUJson) shouldBe false
      Filter.LT("name", 123).test(Map("name" -> "toe-foo").asUJson) shouldBe false
      Filter.LT("name", 123).test(Map("name" -> "bar").asUJson) shouldBe false
    }
  }
  "Filter.LTE" should {
    "filter" in {
      Filter.LTE("name", 123).test(Map("another" -> 10).asUJson) shouldBe false
      Filter.LTE("name", 123).test(Map("name" -> 124).asUJson) shouldBe false
      Filter.LTE("name", 123).test(Map("name" -> 122).asUJson) shouldBe true
      Filter.LTE("name", 123).test(Map("name" -> 123).asUJson) shouldBe true
      Filter.LTE("name", 123).test(Map("name" -> "toe-foo").asUJson) shouldBe false
      Filter.LTE("name", 123).test(Map("name" -> "bar").asUJson) shouldBe false
    }
  }
  "Filter.GTE" should {
    "filter" in {
      Filter.GTE("name", 123).test(Map("another" -> 10).asUJson) shouldBe false
      Filter.GTE("name", 123).test(Map("name" -> 124).asUJson) shouldBe true
      Filter.GTE("name", 123).test(Map("name" -> 122).asUJson) shouldBe false
      Filter.GTE("name", 123).test(Map("name" -> 123).asUJson) shouldBe true
      Filter.GTE("name", 123).test(Map("name" -> "toe-foo").asUJson) shouldBe false
      Filter.GTE("name", 123).test(Map("name" -> "bar").asUJson) shouldBe false
    }
  }
  "Filter.GT" should {
    "filter" in {
      Filter.GT("name", 123).test(Map("another" -> 10).asUJson) shouldBe false
      Filter.GT("name", 123).test(Map("name" -> 124).asUJson) shouldBe true
      Filter.GT("name", 123).test(Map("name" -> 122).asUJson) shouldBe false
      Filter.GT("name", 123).test(Map("name" -> 123).asUJson) shouldBe false
      Filter.GT("name", 123).test(Map("name" -> "toe-foo").asUJson) shouldBe false
      Filter.GT("name", 123).test(Map("name" -> "bar").asUJson) shouldBe false
    }
  }
  "Filter.Match" should {
    "filter" in {
      val expected = 12.withKey("id").merge("foo".withKey("name"))
      Filter.Match(expected).test(expected) shouldBe true
      Filter.Match(expected.merge("additional".withKey("data"))).test(expected) shouldBe false
      Filter.Match(13.withKey("id").merge("foo".withKey("name"))).test(expected) shouldBe false
    }
  }
  "Filter.Not" should {
    "filter" in {
      val expected = 12.withKey("id").merge("foo".withKey("name"))
      Filter.Match(expected).not.test(expected) shouldBe false
      Filter.Match(expected.merge("additional".withKey("data"))).not.test(expected) shouldBe true
      Filter.Match(13.withKey("id").merge("foo".withKey("name"))).not.test(expected) shouldBe true
    }
  }
  "Filter.And" should {
    "filter" in {
      val filter = Filter.GT("value", 10).and(Filter.LT("value", 20))
      filter.test(11.withKey("value")) shouldBe true
      filter.test(19.withKey("value")) shouldBe true
      filter.test(10.withKey("value")) shouldBe false
      filter.test(20.withKey("value")) shouldBe false
      filter.test(15.withKey("missing")) shouldBe false
    }
  }
  "Filter.Or" should {
    "filter" in {
      val filter = Filter.GT("value", 10).or(Filter.LT("another", 20))
      filter.test(11.withKey("value")) shouldBe true
      filter.test(10.withKey("value")) shouldBe false
      filter.test(19.withKey("another")) shouldBe true
      filter.test(20.withKey("another")) shouldBe false
      filter.test(15.withKey("missing")) shouldBe false
    }
  }

  "Filter" should {
    List(
      Contains("foo", "bar"),
      LTE("foo", 123),
      GTE("foo", 456),
      Eq("foo", "bar".asJsonString),
      GTE("foo", 456).not,
      Eq("foo", "bar".asJsonString).and(Eq("foo", "buzz".asJsonString)),
      Eq("foo", "bar".asJsonString).or(Eq("foo", "buzz".asJsonString))
    ).foreach { filter =>
      s"be able to serialise $filter to/from json: ${filter.asUJson.render(0)}" in {
        filter.asUJson.as[Filter] shouldBe Success(filter)
      }
    }
  }
}
