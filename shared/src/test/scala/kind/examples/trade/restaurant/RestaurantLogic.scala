package kind.examples.trade.restaurant

import kind.logic.*
import kind.logic.telemetry.*

enum RestaurantLogic[A]:
  case GetStrategy                                  extends RestaurantLogic[Strategy]
  case CheckInventory(ingredients: Seq[Ingredient]) extends RestaurantLogic[Inventory]
  // making a dish should also update the inventory
  case MakeDish(dish: Dish)                            extends RestaurantLogic[PreparedOrder]
  case UpdateInventory(newInventory: Inventory)        extends RestaurantLogic[Unit]
  case SaveOrder(order: Order)                         extends RestaurantLogic[OrderId]
  case ReplaceStock(ingredients: Map[Ingredient, Int]) extends RestaurantLogic[ReplacementOrderRef]
  case Log(message: String)                            extends RestaurantLogic[Unit]
  case NoOp                                            extends RestaurantLogic[Unit]

object RestaurantLogic:

  import RestaurantLogic.*

  type App[A] = Program[RestaurantLogic, A]

  // format: off
  /**
    * This logic is code for:
    * 1. can we make all the dishes given the current ingredients?
    *    Yes: crack on (continue to step 2)
    *    No: check restock logic (we're probably low) 
    *        if we're NOT low, log that ... we might need to revisit our strategy
    *        and reject the order
    * 2. make the order (dishes) 
    *    update the inventory
    *    check restock logic (do we now need to restock?)
    * 
    * @param order
    */
    // format: on   
  def placeOrder(order: Order): Program[RestaurantLogic, OrderId | OrderRejection] = {
    for
      currentInventoryForIngredientsRequired <- checkInventory(order.orderIngredients)
      required = order.missingInventoryRequired(currentInventoryForIngredientsRequired).toMap
      result <-
        if required.isEmpty then makeTheOrder(order, currentInventoryForIngredientsRequired)
        else rejectOrder(order, currentInventoryForIngredientsRequired)
    yield result
  }

  private def makeTheOrder(
      order: Order,
      currentInventory: Inventory
  ): Program[RestaurantLogic, OrderId] = {
    for
      id <- SaveOrder(order).asProgram
      _  <- makeDishes(OrderId(order.hashCode().toString), order.dishes)
      _  <- UpdateInventory(currentInventory - order.asInventory).asProgram
      _  <- replaceStockIfNecessary(currentInventory, order.asInventory)
    yield id
  }

  private def replaceStockIfNecessary(
      currentInventory: Inventory,
      usedInventory: Inventory
  ): Program[RestaurantLogic, Unit] = {
    for
      strategy: Strategy <- getStrategy
      replacements = usedInventory.toMap.collect {
        case (key, count) if count < strategy.minQuantity =>
          (key, strategy.replacementOrderGivenSize(count))
      }
      _ <- log(s"replacement calculated: $replacements")
      _ <- if replacements.isEmpty then NoOp.asProgram else ReplaceStock(replacements).asProgram
    yield ()
  }

  private def makeDishes(
      orderId: OrderId,
      dishes: Seq[Dish]
  ): Program[RestaurantLogic, OrderId] = {
    dishes match {
      case Nil => Program.of(orderId)
      case next +: theRest =>
        for
          _ <- MakeDish(next).asProgram
          _ <- makeDishes(orderId, theRest)
        yield orderId
    }
  }

  private def rejectOrder(
      order: Order,
      currentInventory: Inventory
  ): Program[RestaurantLogic, OrderRejection] = {
    val rejection = OrderRejection(
      s"Cannot make order $order. Missing ingredients: ${order.missingInventoryRequired(currentInventory)}"
    )
    Program.pure(rejection)
  }

  private def getStrategy = GetStrategy.asProgram

  private def checkInventory(ingredients: Seq[Ingredient]): App[Inventory] = CheckInventory(
    ingredients
  ).asProgram

  private def log(msg: String) = Log(msg).asProgram
