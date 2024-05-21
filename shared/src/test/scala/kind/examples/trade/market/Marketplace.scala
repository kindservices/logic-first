package kind.examples.trade.market

import zio.*
import kind.logic.*
import kind.logic.telemetry.*

import scala.reflect.ClassTag

trait Marketplace:
  def placeOrder(order: Order): Task[OrderId | OutOfStockResponse]

object Marketplace {
  import MarketplaceLogic.*

  val Symbol = Actor.service[Marketplace]

  class App(appLogic: [A] => MarketplaceLogic[A] => Result[A])(using telemetry: Telemetry)
      extends RunnableProgram[MarketplaceLogic](appLogic)
      with Marketplace {

    override protected def appCoords = Symbol

    // I tried to lift this up to RunnableProgram, but apparently doing this with the generic F[_] type
    // was a bridget too far
    def withOverride(overrideFn: PartialFunction[MarketplaceLogic[?], Result[?]]): App = {
      val newLogic: [A] => MarketplaceLogic[A] => Result[A] = [A] => {
        (_: MarketplaceLogic[A]) match {
          case value if overrideFn.isDefinedAt(value) =>
            overrideFn(value).asInstanceOf[Result[A]]
          case value => logic(value).asInstanceOf[Result[A]]
        }
      }
      new App(newLogic)
    }

    override def placeOrder(order: Order): Task[OrderId | OutOfStockResponse] = run(
      MarketplaceLogic.placeOrder(order)
    )
  }

  def apply(how: [A] => MarketplaceLogic[A] => Result[A])(using
      telemetry: Telemetry = Telemetry()
  ): App = new App(how)
}
