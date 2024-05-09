package kind.logic.js

import kind.logic.js.scenarios.*
import kind.logic.js.svg.*
import kind.logic.js.mermaid.MermaidPage
import org.scalajs.dom
import scalatags.JsDom.all.*
import org.scalajs.dom.{HTMLElement, Node, document, html}

import scala.scalajs.js.Dynamic.{global => g, literal => lit}
import scala.concurrent.Future
import scala.concurrent.duration.given
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportTopLevel

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
case class Drawer(parent: HTMLElement) {

  /** The idea is that we can remove elements based on the current state of which apps have been
    * added
    *
    * @param inactiveTabs
    *   this is our filter -- only show the tabs which currently aren't on the screen
    * @return
    *   nowt - this updates the components in-place
    */
  def refresh(inactiveTabs: Set[UIComponent] = UIComponent.values.toSet) = {
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

    UIComponent.values.filter(inactiveTabs.contains).foreach { c =>
      add(c.name, c.function)
    }
  }

  EventBus.activeTabs.subscribe { tabs =>
    val inactive = UIComponent.values.toSet -- tabs
    refresh(inactive)
  }
}
