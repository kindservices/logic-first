package kind.examples.trade.market

import kind.logic.*
import scala.compiletime.ops.string
import scala.concurrent.duration.FiniteDuration

opaque type Item = String
extension (item: Item) {
  def name: String = item
}

opaque type Quantity    = Int
opaque type Price       = Double
opaque type OrderId     = String
opaque type Distributor = String
extension (value: Distributor) def distributorName: String = value

opaque type DistributorRef = String

extension (price: Price) {
  def gbp: Double = price
}

extension (value: Double) {
  def asPrice: Price = value
}

extension (value: Int) {
  def asQuantity: Quantity = value
  def asPrice: Price       = value.toDouble
}
extension (value: String) {
  def asOrderId: OrderId                    = value
  def asItem: Item                          = value
  def asDistributor: Distributor            = value
  def asDistributorOrderRef: DistributorRef = value
}

case class Address(street: String, city: String, postCode: String)
case class Order(basket: Map[Item, Quantity], deliveryAddress: Address)
case class RFQResponse(distributor: Distributor, pricesByItem: Map[Item, Price])

case class OutOfStockResponse(items: Set[Item])

case class Settings(maxTimeToRespond: FiniteDuration, fullfillmentAddress: Address)

case class DistributorOrder(distributor: Distributor, orderPortion: Order, orderId: OrderId)

/** Our marketplace knows about distributors. It will get the cheapest quote for an order, and then
  * complete the order by collating all the deliveries of the sub orders.
  *
  * This is a good test-case for logic-first, because the implementation will send out RFQs in
  * parallel, while also timing-out any ones who don't respond in time.
  */
enum MarketplaceLogic[A]:
  case GetConfig               extends MarketplaceLogic[Settings]
  case SaveOrder(order: Order) extends MarketplaceLogic[OrderId]
  case SaveDistributors(orderId: OrderId, sentTo: Map[Distributor, DistributorRef])
      extends MarketplaceLogic[Unit]
  // this will send out RFQs in batch to all our known suppliers
  case SendOutRequestsForQuote(order: Order) extends MarketplaceLogic[Seq[RFQResponse]]
  case SendOrders(orders: Seq[DistributorOrder])
      extends MarketplaceLogic[Map[Distributor, DistributorRef]]

object MarketplaceLogic:

  import MarketplaceLogic.*

  type App[A] = Program[MarketplaceLogic, A]

  /** Our logic is simple - we ask a bunch of suppliers for their quotes on an order, then send the
    * parts of the order to the cheapest supplier, and return the completed order
    *
    * @param order
    * @return
    *   a program which returns either an out-of-stock response, or the order id
    */
  def placeOrder(order: Order): App[OutOfStockResponse | OrderId] = {
    for
      quotes <- SendOutRequestsForQuote(order).asProgram
      result <- splitOrders(order, quotes)
    yield result
  }

  /** Take the quotes and either send them out, or let the customer know one of their items is out
    * of stock.
    *
    * Don't send out partials ... the customer might want to cancel the whole order
    *
    * @param originalOrder
    * @param quotes
    * @return
    */
  def splitOrders(
      originalOrder: Order,
      quotes: Seq[RFQResponse]
  ): App[OutOfStockResponse | OrderId] = {
    val quotedItems  = quotes.flatMap(_.pricesByItem.keySet).toSet
    val missingItems = originalOrder.basket.keySet.diff(quotedItems)
    if missingItems.nonEmpty then {
      Program.of(OutOfStockResponse(missingItems))
    } else {

      def batched(sendTo: Address, orderId: OrderId): Seq[DistributorOrder] = {
        val allOrders: Set[DistributorOrder] = originalOrder.basket.keySet.map { item =>
          val cheapestQuote =
            quotes.filter(_.pricesByItem.contains(item)).minBy(_.pricesByItem(item).gbp)

          val quantity = originalOrder.basket(item)
          DistributorOrder(
            cheapestQuote.distributor,
            Order(Map(item -> quantity), sendTo),
            orderId
          )
        }

        allOrders.groupBy(_.distributor).toSeq.map { (distributor, orders) =>
          val collated = Order(orders.map(_.orderPortion.basket).reduce(_ ++ _), sendTo)
          DistributorOrder(distributor, collated, orderId)
        }
      }

      for
        settings <- GetConfig.asProgram
        orderId  <- SaveOrder(originalOrder).asProgram
        distributorOrders = batched(settings.fullfillmentAddress, orderId)
        refs <- SendOrders(distributorOrders).asProgram
        _    <- SaveDistributors(orderId, refs).asProgram
      yield orderId
    }
  }
