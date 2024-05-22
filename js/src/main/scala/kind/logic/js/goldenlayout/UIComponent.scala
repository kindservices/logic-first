package kind.logic.js.goldenlayout

import kind.logic.js.EventBus
import org.scalajs.dom.Node
import kind.logic.json._

/** These components know how to render themselves in the golden layout.
  * 
  * This was created because there were too many things to change when adding new golden layout components:
  *  $ registering them with golden layout
  *  $ exporting the render function
  *  $ keeping track of all the possible components
  *  $ docs ... just yuck. Too much shit.
  * 
  * So instead, there is:
  * $ our UIComponent which keeps track of all registerd compoents
  * $ The extension method 'addMenuItem' on GoldenLayout for adding new UIComponents
  * $ the kind.logic.js.createNewComponent then knows how to call the 'render' function for the right UIComponent
  * 
  * This makes the usage-site arguably lot simpler in your application.
  * 
  * You end up with something like this in your main block:
  * {{{
  *  @JSExportTopLevel("initLayout")
  *  def initLayout(myLayout: GoldenLayout) = {
  *   val svg = myLayout.addMenuItem(drawer, "SVG") { state =>
  *      MainPage.svgContainer
  *   }
  * }}}
  * 
  * where the 'MainPage.svgContainer' typically creates (lazily) a div or something while also subscribing to events using the EventBus
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
