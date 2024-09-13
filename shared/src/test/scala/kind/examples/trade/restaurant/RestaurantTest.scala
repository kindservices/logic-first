package kind.examples.trade.restaurant

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import kind.logic.*
import kind.logic.telemetry.*
import kind.examples.trade.*

class RestaurantTest extends AnyWordSpec with Matchers {

  "Restaurant.placeOrder" should {
    "restock when we get low on stuff" in {
      given telemetry: Telemetry = Telemetry()

      val testData = new RestaurantTestData
      import testData.*

      calls.foreach(println)

      println("Mermaid:")
      println(telemetry.mermaid.diagram().execOrThrow())

      result shouldBe OrderId("order-2")
    }
  }
}
