package kind.logic.js

// TODO - this shouldn't be an enum ... we should be able to create these dynamically
enum UIComponent(val name: String, val function: String):
  case ScenarioBuilder extends UIComponent("🏗️ Scenario Builder", "createScenarioBuilder")
  case SequenceDiagram extends UIComponent("⮂ Sequence Diagram", "createSequenceDiagram")
  case Interactive     extends UIComponent("▷ Interactive", "createInteractivePage")
  case Diff            extends UIComponent("Δ Diff", "createDiffPage")

object UIComponent {

  def byFunction(f: String): Option[UIComponent] = values.find(_.function == f)

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
