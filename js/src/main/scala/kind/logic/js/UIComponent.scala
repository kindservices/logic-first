package kind.logic.js

import scala.collection.MapView

/** UI Components are things we can represent in the menu.
  *
  * NOTE: THIS MATCHES UP WITH INDEX.HTML
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
