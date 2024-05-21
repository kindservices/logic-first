package kind.logic.js

import kind.logic.js.goldenlayout._
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scalatags.JsDom.all._

import scala.scalajs.js.JSON

/** TODO:
  * {{{
  * $ show last result in scenario builder
  * $ stack component
  * $ errors go to Footer
  * $ SVG tooltips, resizes
  * $ save the golden layout state for refresh
  * }}}
  * @param parent
  *   the parent element
  */
case class Drawer(parent: HTMLElement, myLayout: GoldenLayout) {

  /** The idea is that we can remove elements based on the current state of which apps have been
    * added
    *
    * @param inactiveTabs
    *   this is our filter -- only show the tabs which currently aren't on the screen
    * @return
    *   nowt - this updates the components in-place
    */
  def refresh(inactiveTabs: Set[UIComponent] = UIComponent.values()) = {
    parent.innerHTML = ""

    def add(title: String, function: String) = {

      val config = JSON.parse(s"""{
          "type": "component",
          "componentName": "$function",
          "title": "$title",
          "componentState": {
              "text": "text"
          }}""")

      val item = li(cls := "draggable", title).render
      parent.appendChild(item)
      myLayout.createDragSource(item, config)
    }

    UIComponent.values().filter(inactiveTabs.contains).foreach { c =>
      add(c.name, c.function)
    }
  }

  EventBus.activeTabs.subscribe { tabs =>
    val inactive = UIComponent.values() -- tabs
    refresh(inactive)
  }
}
