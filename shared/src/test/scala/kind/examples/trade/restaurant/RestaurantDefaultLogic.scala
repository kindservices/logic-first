package kind.examples.trade.restaurant

import kind.logic.*
import kind.examples.trade.*

object RestaurantDefaultLogic extends RestaurantDefaultLogic

trait RestaurantDefaultLogic {

  val defaultLogic: [A] => RestaurantLogic[A] => Result[A] = [A] => {
    (_: RestaurantLogic[A]) match {
      case RestaurantLogic.CheckInventory(ingredients) =>
        Inventory(ingredients).asResultTraced(Container.service("inventory", "InventoryService"))
      case RestaurantLogic.MakeDish(dish) =>
        PreparedOrder(dish, OrderId("1")).asResult
      case RestaurantLogic.UpdateInventory(newInventory) =>
        ().asResultTraced(Container.service("inventory", "InventoryService"))
      case RestaurantLogic.ReplaceStock(ingredients) =>
        s"replace-${ingredients.size}".asReplacementOrderRef
          .asResultTraced(Container("supplier", "Marketplace").service)
      case RestaurantLogic.Log(message) =>
        Console.println(message).asResult
      case RestaurantLogic.NoOp => Console.println("no-op").asResult
      case RestaurantLogic.GetStrategy =>
        Strategy(30, 7).asResultTraced(Container.serviceForClass[Restaurant])
      case RestaurantLogic.SaveOrder(order) =>
        OrderId(s"order-${order.dishes.size}").asResultTraced(Container.serviceForClass[Restaurant])
    }
  }
}
