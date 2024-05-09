package kind.examples.trade

import kind.logic.*

package object restaurant {

  // we perhaps have an enum of ingredients (there are only so many things) which can be used to make food
  // but here we just represent them as strings
  opaque type Ingredient = String
  object Ingredient:
    def apply(value: String): Ingredient = value
  extension (ingredient: Ingredient) def name: String = ingredient

  // we represent a dish (plate of food) as a collection of ingredients involved
  opaque type Dish = Seq[Ingredient]
  object Dish:
    def apply(first: Ingredient, theRest: Ingredient*): Dish = first +: theRest.toSeq
    def apply(value: Seq[Ingredient]): Dish                  = value
  extension (dish: Dish)
    def dishIngredients: Seq[Ingredient] = dish

    def inventoryRequired: Inventory = {
      dish
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toMap
    }
    // given the current inventory, return a new inventory where the ingredient counts are the number
    // of ingredients needed beyond what we currently have.
    //
    // for example, if this dish has 3 eggs, and our inventory contains 2 eggs, then we need 1 more egg
    def remainingIngredientsRequired(inventory: Inventory): Inventory = {
      dish.inventoryRequired.toMap
        .map { case (ingredient, count) =>
          val currentCount = inventory.getOrElse(ingredient, 0)
          val remaining    = count - currentCount
          (ingredient, remaining)
        }
        .filter((_, value) => value < 0)
        .map((k, v) => (k, v.abs))
        .toMap
    }

  // an order is just a list of dishes ... table of four?
  opaque type Order = Seq[Dish]
  object Order:
    def apply(first: Dish, theRest: Dish*): Order = first +: theRest.toSeq
    def apply(value: Seq[Dish]): Order            = value

  extension (order: Order)
    def orderIngredients: Seq[Ingredient] = order.flatten
    def asInventory: Inventory            = Inventory(orderIngredients)
    def dishes: Seq[Dish]                 = order
    def inventoryRequired(inventory: Inventory): Inventory =
      order.foldLeft(Inventory.empty) { case (acc, dish) =>
        acc ++ dish.remainingIngredientsRequired(inventory)
      }
    def missingInventoryRequired(inventory: Inventory): Inventory =
      order.foldLeft(Inventory.empty) { case (acc, dish) =>
        acc ++ dish.remainingIngredientsRequired(inventory)
      }

  case class PreparedOrder(dish: Dish, orderId: OrderId)

  opaque type ReplacementOrderRef = String
  extension (ref: String) def asReplacementOrderRef: ReplacementOrderRef = ref

  opaque type OrderId = String
  object OrderId:
    def apply(value: String): OrderId = value

  // how should the restaurant handle stock?
  case class Strategy(minQuantity: Int, batchReplacementSize: Int) {
    // given the current inventory size, return the size of the order we need to make
    def replacementOrderGivenSize(currentSize: Int) = {
      if (currentSize > minQuantity) then 0
      else {
        val numRequired = minQuantity - currentSize
        val batches     = numRequired / batchReplacementSize
        batches.max(1) * batchReplacementSize
      }
    }
  }

  case class OrderRejection(reason: String)

  opaque type Inventory = Map[Ingredient, Int]
  object Inventory:
    def empty                                         = Map.empty[Ingredient, Int]
    def apply(value: Map[Ingredient, Int]): Inventory = value
    def apply(fromIngredients: Seq[Ingredient]): Inventory =
      val map: Map[Ingredient, Int] = fromIngredients.groupBy(identity).view.mapValues(_.size).toMap
      map
  extension (inventory: Inventory)
    def -(other: Inventory): Inventory = other.foldLeft(inventory) { case (updated, (key, count)) =>
      updated.get(key) match {
        case Some(c) => updated.updated(key, c - count)
        case None    =>
          // we're subtracting something which wasn't even in our inventory
          updated
      }
    }
    def isEmpty                     = inventory.isEmpty
    def toMap: Map[Ingredient, Int] = inventory

}
