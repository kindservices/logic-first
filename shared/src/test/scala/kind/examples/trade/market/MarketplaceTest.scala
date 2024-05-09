package kind.examples.trade.market

import kind.logic.{*, given}
import kind.logic.telemetry.*
import kind.examples.trade.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.*

import scala.concurrent.duration.*

class MarketplaceTest extends AnyWordSpec with Matchers {

  import MarketplaceLogic.*

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
