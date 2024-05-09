package kind.logic.js

import kind.logic.*
import scala.collection.mutable.HashMap
import scala.util.chaining.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** poor-man's event bus
  */
class EventBus[A] {

  private var id            = 0
  private val listenersById = HashMap[String, A => Unit]()

  def publish(event: A) = listenersById.values.foreach(it => Future(it(event)))

  def unsubscribe(key: String) = listenersById.remove(key)

  def subscribe(onEvent: A => Unit): String = {
    id += 1
    id.toString.tap { id =>
      listenersById(id) = onEvent
    }
  }

}

/** Holds some naughty, app-wide event busses
  */
object EventBus {

  // published when tabs are opened
  val tabOpen = new EventBus[UIComponent]

  // published when tabs are closed
  val tabClosed = new EventBus[UIComponent]

  // published when the active set of tabs changes
  val activeTabs = new EventBus[Set[UIComponent]]

  // publishes when the active test scenario changes
  val activeTestScenario = new EventBus[TestScenario]
}
