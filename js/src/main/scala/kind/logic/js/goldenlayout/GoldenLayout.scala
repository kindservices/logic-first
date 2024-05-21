package kind.logic.js.goldenlayout

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSGlobal("GoldenLayout")
class GoldenLayout(config: js.Any, container: dom.Element) extends js.Object {
  def init(): Unit                                                     = js.native
  def registerComponent(name: String, component: js.Function): Unit    = js.native
  def createDragSource(element: dom.Element, itemConfig: js.Any): Unit = js.native
}
