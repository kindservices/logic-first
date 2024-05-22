package kind.logic.js.goldenlayout

import kind.logic.js.EventBus
import org.scalajs.dom.{HTMLElement, Node}
import kind.logic.json._

/** These components know how to render themselves in the golden layout.
  *
  * This was created because there were too many things to change when adding new golden layout
  * components: $ registering them with golden layout $ exporting the render function $ keeping
  * track of all the possible components $ docs ... just yuck. Too much shit.
  *
  * So instead, there is: $ our UIComponent which keeps track of all registerd compoents $ The
  * extension method 'addMenuItem' on GoldenLayout for adding new UIComponents $ the
  * kind.logic.js.createNewComponent then knows how to call the 'render' function for the right
  * UIComponent
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
  * where the 'MainPage.svgContainer' typically creates (lazily) a div or something while also
  * subscribing to events using the EventBus
  *
  * @param menuItem
  *   the menuItem associated with this component
  * @param state
  *   the initial component state (which will have 'id' and 'title', amongst other things)
  * @param render
  *   the render function which gets called by kind.logic.js.createNewComponent
  */
class UIComponent private (val menuItem: HTMLElement, val state: State, val render: State => Node) {
  def id    = state("id").str
  def title = state("title").str

  private var previousDisplayValue = ""

  def hideMenuItem() = {
    if previousDisplayValue.isEmpty then previousDisplayValue = menuItem.style.display
    menuItem.style.display = "none"
  }

  def showMenuItem() = {
    if previousDisplayValue.nonEmpty then menuItem.style.display = previousDisplayValue
  }

  override def hashCode(): Int = state.hashCode
  override def equals(other: Any): Boolean = other match {
    case that: UIComponent => this.state == that.state
    case _                 => false
  }
  override def toString = s"UIComponent(${state.render(2)})"
}

object UIComponent {

  private var componentIds = Map.empty[String, UIComponent]

  def get(id: String) = componentIds.get(id)

  def default() = componentIds.values.headOption

  /** creates a new UIComponent and registers it globally.
    *
    * @param menuItem
    * @param title
    * @param initialState
    * @param render
    * @return
    */
  private[goldenlayout] def register(
      menuItem: HTMLElement,
      title: String,
      initialState: State,
      render: State => Node
  ) = {
    val id    = s"elm-${title.filter(_.isLetterOrDigit)}-${componentIds.size}"
    val state = initialState.mergeWith(ujson.Obj("title" -> title, "id" -> id))

    val comp = UIComponent(menuItem, state, render)
    componentIds += id -> comp
    comp
  }

  private var active = Set[UIComponent]()

  /** @return
    *   all currnetly active components (as determined by the EventBus.tabOpen and
    *   EventBus.tabClosed events)
    */
  def activeComponents(): Set[UIComponent] = active

  /** @return
    *   all currnetly inactive components (as determined by the EventBus.tabOpen and
    *   EventBus.tabClosed events)
    */
  def inactiveComoponents(): Set[UIComponent] = componentIds.values.filterNot(active.contains).toSet

  EventBus.tabOpen.subscribe { c =>
    active += c
    EventBus.activeTabs.publish(active)
  }

  EventBus.tabClosed.subscribe { c =>
    active -= c
    EventBus.activeTabs.publish(active)
  }
}
