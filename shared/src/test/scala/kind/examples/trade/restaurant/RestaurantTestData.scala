package kind.examples.trade.restaurant

import kind.logic.*
import kind.logic.telemetry.*
import kind.examples.trade.*

object RestaurantTestData {

  val pancake = Dish(
    Ingredient("milk"),
    Ingredient("butter"),
    Ingredient("flour"),
    Ingredient("egg"),
    Ingredient("egg"),
    Ingredient("egg")
  )

  val fishAndChips = Dish(Ingredient("fish"), Ingredient("chips"))

  val order = Order(List(pancake, fishAndChips))
}
class RestaurantTestData(using telemetry: Telemetry = Telemetry()) extends RestaurantDefaultLogic {
  import RestaurantTestData.*
  val restaurant = Restaurant(defaultLogic)

  lazy val result = restaurant.placeOrder(order).execOrThrow()

  def calls: Seq[CompletedCall] = telemetry.calls.execOrThrow()

}
