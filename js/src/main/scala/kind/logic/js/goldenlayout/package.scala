package kind.logic.js

import org.scalajs.dom.{Node, HTMLElement}
import scala.scalajs.js.JSON
import scala.scalajs.js
import scalatags.JsDom.all._

package object goldenlayout {
  type State = ujson.Value

  extension (myLayout: GoldenLayout) {

    def add(
        drawer: Node,
        title: String,
        initialState: State = ujson.Null,
        menuItem: HTMLElement = null
    )(render: State => Node) = {

      val comp = UIComponent.register(title, initialState, render)
      val item = Option(menuItem).getOrElse(li(cls := "draggable", title).render)

      val config = JSON.parse(s"""{
                "type": "component",
                "componentName": "createNewComponent",
                "title": "$title",
                "componentState": ${comp.state.render(0)} 
            }""")

      myLayout.createDragSource(item, config)

      drawer.appendChild(item)
    }
  }
}
