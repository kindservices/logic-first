package kind.logic.js

import scala.collection.MapView

/** UI Components are things we can represent in the menu.
  *
  * To add new UIComponents to the menu, we need to:
  *
  * #1 create a function with the following signature and export with JSExportTopLevel
  * 
  * {{{
  * @scala.scalajs.js.annotation.JSExportTopLevel("createDb")
  * def createDb(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) = { 
  *   val db = TableComponent(Map("foo" -> ujson.read("""{"bar": 1}""")))
  *   container.replace(db.element.execOrThrow())
  * }
  * }}}
  * 
  * #2 register the component with UIComponent.register so the Drawer knows about it
  * {{{
  *  UIComponent.register(UIComponent("DB", "createDb"))
  * }}}
  * 
  * #3 Export the function in the window global scope:
  * {{{
  *  global.window.createScenarioBuilder = createDb
  * }}}
  * 
  * #4
  * Register the function with GoldenLayer in index.html
  * {{{
  * 
  * myLayout.registerComponent('createDb', function(container, state) {
  *      createDb(container, state);
  * });
  * }}}
  *
  * NOTE: We could eliminate step 3 and 4 if we taught UIComponent about window.global.myLayout!
  * 
  * We can drag them onto the screen
  *
  * @param name
  *   the friendly name of the component
  * @param function
  *   the exported function used to render the component -- this function should correspond to a
  *   change in the index.html
  */
case class UIComponent(val name: String, val function: String)

object UIComponent {

  val ScenarioBuilder = UIComponent("🏗️ Scenario Builder", "createScenarioBuilder")
  val SequenceDiagram = UIComponent("⮂ Sequence Diagram", "createSequenceDiagram")
  val Interactive     = UIComponent("▷ Interactive", "createInteractivePage")
  val Diff            = UIComponent("Δ Diff", "createDiffPage")

  private var byFunctionMap: Map[String, UIComponent] = Seq(
    ScenarioBuilder,
    SequenceDiagram,
    Interactive,
    Diff
  ).groupBy(_.function).view.mapValues(_.head).toMap

  def values(): Set[UIComponent] = byFunctionMap.values.toSet

  def register(c: UIComponent) = {
    byFunctionMap = byFunctionMap.updated(c.function, c)
  }

  def byFunction(f: String): Option[UIComponent] = byFunctionMap.get(f)

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
