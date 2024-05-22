package kind.logic.js.goldenlayout

import kind.logic.js.EventBus
import org.scalajs.dom.Node
import kind.logic.json._

/** These components know how to render themselves in the golden layout
  *
  * @param state
  * @param render
  */
class UIComponent(val state: State, val render: State => Node) {
  def id    = state("id").str
  def title = state("title").str

  override def hashCode(): Int = state.hashCode
  override def equals(other : Any): Boolean = other match {
    case that: UIComponent => this.state == that.state
    case _ => false
  }
  override def toString = s"UIComponent(${state.render(2)})"
}

object UIComponent {

  private var componentIds = Map.empty[String, UIComponent]

  def get(id: String) = componentIds.get(id)

  def default() = componentIds.values.headOption

  def register(title: String, initialState: State, render: State => Node) = {
    val id    = s"elm-${title.filter(_.isLetterOrDigit)}-${componentIds.size}"
    val state = initialState.mergeWith(ujson.Obj("title" -> title, "id" -> id))

    val comp = UIComponent(state, render)
    componentIds += id -> comp
    comp
  }

  private var active = Set[UIComponent]()

  EventBus.tabOpen.subscribe { c =>
    active += c
    EventBus.activeTabs.publish(active)
  }

  EventBus.tabClosed.subscribe { c =>
    active -= c
    EventBus.activeTabs.publish(active)
  }
}
