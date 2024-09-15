package kind.examples.trade.restaurant

import kind.examples.trade.*
import zio.*
import kind.logic.*
import kind.logic.telemetry.*

trait Restaurant {
  def placeOrder(order: Order): Task[OrderId | OrderRejection]
}

object Restaurant {

  val Symbol = Container.service[Restaurant]

  class App(appLogic: [A] => RestaurantLogic[A] => Result[A])(using telemetry: Telemetry)
      extends RunnableProgram[RestaurantLogic](appLogic)
      with Restaurant {

    override protected def appCoords = Symbol

    // I tried to lift this up to RunnableProgram, but apparently doing this with the generic F[_] type
    // was a bridget too far
    def withOverride(overrideFn: PartialFunction[RestaurantLogic[?], Result[?]]): App = {
      val newLogic: [A] => RestaurantLogic[A] => Result[A] = [A] => {
        (_: RestaurantLogic[A]) match {
          case value if overrideFn.isDefinedAt(value) =>
            overrideFn(value).asInstanceOf[Result[A]]
          case value => logic(value).asInstanceOf[Result[A]]
        }
      }
      new App(newLogic)
    }

    override def placeOrder(order: Order): Task[OrderId | OrderRejection] = run(
      RestaurantLogic.placeOrder(order)
    )
  }
  def apply(how: [A] => RestaurantLogic[A] => Result[A])(using telemetry: Telemetry = Telemetry()) =
    new App(RestaurantDefaultLogic.defaultLogic)
}
