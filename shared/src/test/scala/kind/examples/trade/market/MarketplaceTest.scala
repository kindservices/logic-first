package kind.examples.trade.market

import kind.logic.*
import kind.logic.telemetry.*
import kind.examples.trade.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MarketplaceTest extends AnyWordSpec with Matchers {

  "Marketplace" should {
    "trace orders" in {

      given telemetry: Telemetry = Telemetry()
      val testData               = new MarketplaceTestData
      import testData.*

      println(result)
      telemetry.calls.execOrThrow().foreach(println)
    }
  }
}
