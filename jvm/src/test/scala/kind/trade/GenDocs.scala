package kind.examples.trade

import kind.logic.telemetry.*
import kind.logic.jvm.*
import kind.logic.*
import kind.examples.trade.restaurant.{Order as ROrder, *}
import kind.examples.trade.market.*
import eie.io.{*, given}
import kind.examples.trade

import scala.language.implicitConversions

def restaurantFlow = {

  def asMermaid(input: ROrder) = {
    // val input = Order(basket, Address("unit", "test", "ave"))
    given telemetry: Telemetry = Telemetry()

    val testData = new RestaurantTestData
    import testData.*

    // testData.result.ensuring(_ != null) // <-- we have to evaluate this / run

    restaurant.placeOrder(input).execOrThrow()
    telemetry.asMermaidDiagram().execOrThrow()
  }

  Scenario("Restaurant", RestaurantTestData.order, asMermaid)
}

def marketFlow = {
  def asMermaid(input: Order) = {
    given telemetry: Telemetry = Telemetry()

    val testData = new MarketplaceTestData

    testData.underTest.placeOrder(input).execOrThrow()
    telemetry.calls.execOrThrow().foreach(println)

    telemetry.asMermaidDiagram().execOrThrow()
  }
  Scenario("Marketplace", MarketplaceTestData.input, asMermaid)
}

def endToEndFlow = {

  //
  def asMermaid(order: ROrder) = {
    given telemetry: Telemetry = Telemetry()

    val marketPlaceSetup = new MarketplaceTestData
    val restaurantSetup  = new RestaurantTestData

    val endToEnd = restaurantSetup.restaurant.withOverride {
      case RestaurantLogic.ReplaceStock(inventory) =>
        val asBasket = inventory.map { case (ingredient: Ingredient, quantity: Int) =>
          val key: Item       = ingredient.name.asItem
          val value: Quantity = quantity.asQuantity
          (key, value)
        }

        val replacementOrder = Order(asBasket.toMap, Address("The", "Restaurant", "Address"))
        marketPlaceSetup.underTest
          .placeOrder(replacementOrder)
          .map { orderId =>
            orderId.toString.asDistributorOrderRef
          }
          .taskAsResultTraced(Marketplace.Symbol)

    }

    val result = endToEnd.placeOrder(order).execOrThrow()
    telemetry.calls.execOrThrow().foreach(println)
    telemetry.asMermaidDiagram().execOrThrow()
  }

  Scenario("End to End", RestaurantTestData.order, asMermaid)
}

@main def genDocs() = {

  val scenarios = List(
    restaurantFlow,
    marketFlow,
    endToEndFlow
  )

}
