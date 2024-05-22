package kind.logic

import kind.logic.js.goldenlayout.UIComponent
import scalatags.JsDom.all.*
import org.scalajs.dom.Node
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.global

import ujson.Value

package object js {
  type Json = ujson.Value

  import scala.scalajs.js.annotation.JSExportTopLevel

  /** This is our single function for rendered any new component, rather than the typical
    * GoldenLayout way of doing it.
    *
    * This was due to the amount of duplication/effort in having to export new functions, registerer
    * them, etc.
    *
    * See the docs on UIComponent and the GoldenLayout 'addMenuItem' extension function for more
    * info.
    *
    * @param container
    *   the container of this component
    * @param state
    *   the component state
    */
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
            container.setTitle("Dev Usage Error: No components registered")
            container.replace(
              div(
                "Dev Usage Issue: No components registered. Use myLayout.addMenuItem(...) in your initLayout function"
              ).render
            )
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

  // these are used in index.html as we set up globallayout (e.g. myLayout)
  global.window.createNewComponent = createNewComponent
  global.window.onComponentDestroyed = onComponentDestroyed
  global.window.onComponentCreated = onComponentCreated
}
