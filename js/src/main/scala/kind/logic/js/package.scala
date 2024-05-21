package kind.logic

import kind.logic.js.goldenlayout.UIComponent
import scalatags.JsDom.all.*
import org.scalajs.dom.Node
import scala.scalajs.js.JSON

import ujson.Value

package object js {
  type Json = ujson.Value

  /** This layout is exported in main.js for us to reference here */
  // def myLayout = scala.scalajs.js.Dynamic.global.window.myLayout

  import scala.scalajs.js.annotation.JSExportTopLevel

  @JSExportTopLevel("createNewComponent")
  def createNewComponent(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) = {
    val stateJS = ujson.read(JSON.stringify(state))
    kind.logic.js.goldenlayout.UIComponent.get(s"${state.id}") match {
      case Some(comp) =>
        container.setTitle(s"${state.title}")
        container.replace(comp.render(stateJS))
      case None =>
        kind.logic.js.goldenlayout.UIComponent.default() match {
          case Some(comp) =>
            container.setTitle(comp.title)
            container.replace(comp.render(stateJS))
          case None =>
            container.setTitle("No components registered")
            container.replace(div("No components registered").render)
        }
    }
  }

// this is used to update the menu
  @JSExportTopLevel("onComponentCreated")
  def onComponentCreated(id: String) = UIComponent.get(id).foreach(EventBus.tabOpen.publish)

  @JSExportTopLevel("onComponentDestroyed")
  def onComponentDestroyed(id: String) =
    UIComponent.get(id).foreach(EventBus.tabClosed.publish)

  extension (container: scala.scalajs.js.Dynamic) {
    def placeholder(name: String, state: scala.scalajs.js.Dynamic) = {
      container.title = name
      container
        .getElement()
        // .html(div(h2(cls := "subtitle", s"${name}!"), div(JSON.stringify(state))).render)
        .html(div(h2(cls := "subtitle", s"${name}!"), div(state.toString)).render)
    }

    def replace(node: Node) = container.getElement().html(node)
  }

  extension (jason: String) {
    def asUJson: Value = ujson.read(jason)
    def asJSON         = JSON.parse(jason)
  }

  // for each stack in a test call frame, there is an input and an output
  type StackElement = (Json, Json)


  global.window.initLayout = initLayout
  global.window.createNewComponent = createNewComponent

  global.window.onComponentDestroyed = onComponentDestroyed
  global.window.onComponentCreated = onComponentCreated
}
