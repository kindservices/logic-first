package kind.examples.trade.market

import kind.logic.{given, *}
import kind.logic.telemetry.*
import kind.examples.trade.*
import kind.examples.trade.market.MarketplaceLogic.*
import scala.concurrent.duration.*

object MarketplaceTestData {
  val basket: Map[Item, Quantity] = Map(
    ("eggs".asItem: Item)    -> (3.asQuantity: Quantity),
    ("brocoli".asItem: Item) -> (1.asQuantity: Quantity)
  )

  val input = Order(basket, Address("unit", "test", "ave"))
}

class MarketplaceTestData(using telemetry: Telemetry = Telemetry()) extends MarketplaceTestLogic {
  import MarketplaceTestData.*

  val underTest = Marketplace(defaultLogic).withOverride { case GetConfig =>
    Settings(1.seconds, Address("Override", "Street", "Eyam")).asResultTraced(
      Marketplace.Symbol.withName("Config")
    )
  }

  lazy val result =
    underTest.placeOrder(input).execOrThrow()
}
