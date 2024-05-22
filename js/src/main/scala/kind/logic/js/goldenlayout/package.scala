package kind.logic.js

import org.scalajs.dom.{Node, HTMLElement}
import scala.scalajs.js.JSON
import scala.scalajs.js
import scalatags.JsDom.all._

package object goldenlayout {
  type State = ujson.Value

  /** We can't add this directly to a js.Object which isn't a js.native thing, so we do it via an
    * extension method here
    */
  extension (myLayout: GoldenLayout) {

    /** Convenience method to create a new UIComponent (globally registered) and draggable menu
      * item.
      *
      * This works by having a single 'kind.logic.js.createNewComponent' (see the package object).
      *
      * That one function will delegate to the UIComponent with the same id as the state.id field.
      *
      * @param navigation
      *   the place where we'll append our new menu item
      * @param title
      *   the title of the menu item
      * @param initialState
      *   the initial state of the component. This will be updated with a 'title' and 'id' field
      * @param menuItem
      *   the menu item to add (null defaults to a new li element with the title as text)
      * @param render
      *   the function which will draw the body of the component
      */
    def addMenuItem(
        navigation: Node,
        title: String,
        initialState: State = ujson.Null,
        menuItem: HTMLElement = null
    )(render: State => Node): UIComponent = {

      val item = Option(menuItem).getOrElse(li(cls := "draggable", title).render)

      val comp = UIComponent.register(item, title, initialState, render)

      val config = JSON.parse(s"""{
                "type": "component",
                "componentName": "createNewComponent",
                "title": "$title",
                "componentState": ${comp.state.render(0)} 
            }""")

      myLayout.createDragSource(item, config)

      navigation.appendChild(item)

      comp
    }
  }
}
