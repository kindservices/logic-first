package kind.logic.js

import kind.logic.*
import kind.logic.js.svg.*
import kind.logic.telemetry.*
import kind.logic.js.scenarios.*
import kind.logic.js.mermaid.*
import org.scalajs.dom
import scalatags.JsDom.all.*
import org.scalajs.dom.*

import scala.scalajs.js.Dynamic.global
import scala.concurrent.Future
import scala.concurrent.duration.given
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportTopLevel
import upickle.default.*
import scala.util.control.NonFatal

object AppSkeleton {

  lazy val mermaidPage = MermaidPage()

  lazy val svgPage: HTMLDivElement = div(div("SVG Page")).render
}

// lazy val mermaidPage = {
//   val page = MermaidPage()

//   EventBus.activeTestScenario.subscribe { scenario =>
//     try {
//       val request         = read[CreateContractRequest](scenario.input)
//       val mermaidMarkdown = CreateDraftAsMermaid(request)
//       page.update(scenario, mermaidMarkdown)
//     } catch {
//       case NonFatal(e) =>
//         page.updateError(scenario, s"We couldn't parse the scenario as a DraftContract: $e")
//     }
//   }
//   page.element
// }

// def pizzaAsSvg(scenario: TestScenario): Option[HTMLDivElement] = {
//   try {
//     val request                = scenario.inputAs[MakePizzaRequest](scenario.input)
//     given telemetry: Telemetry = Telemetry()
//     val result =
//       PizzaOps.defaultProgram.orderPizza(request.quantity, request.toppings).execOrThrow()
//     val calls = telemetry.calls.execOrThrow()
//     Option(SvgForCalls(calls))
//   } catch {
//     case NonFatal(e) =>
//       println(s"Error creating svg: $e")
//       None
//   }
// }

// def initSvg() = {
//   EventBus.activeTestScenario.subscribe { scenario =>
//     def fallback  = div(s"We couldn't parse the scenario as a DraftContract or Restaurant").render
//     val component = pizzaAsSvg(scenario).getOrElse(fallback)
//     AppSkeleton.svgPage.innerHTML = ""
//     AppSkeleton.svgPage.appendChild(component)
//   }
// }

// @JSExportTopLevel("createScenarioBuilder")
// def createScenarioBuilder(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
//   container.replace(ScenarioBuilder().content)

// @JSExportTopLevel("createSequenceDiagram")
// def createSequenceDiagram(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
//   container.replace(AppSkeleton.mermaidPage.element)

// @JSExportTopLevel("createInteractivePage")
// def createInteractivePage(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
//   container.replace(AppSkeleton.svgPage)

// @JSExportTopLevel("createDiffPage")
// def createDiffPage(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
//   container.placeholder("Diff", state)

// this is used to update the menu
@JSExportTopLevel("onComponentCreated")
def onComponentCreated(id: String) = UIComponent.byFunction(id).foreach(EventBus.tabOpen.publish)

@JSExportTopLevel("onComponentDestroyed")
def onComponentDestroyed(id: String) =
  UIComponent.byFunction(id).foreach(EventBus.tabClosed.publish)

// @main
// def layout(): Unit = {
//   new Drawer(HtmlUtils.$("drawer")).refresh()

//   global.window.createScenarioBuilder = createScenarioBuilder
//   global.window.createSequenceDiagram = createSequenceDiagram
//   global.window.createInteractivePage = createInteractivePage
//   global.window.createDiffPage = createDiffPage

//   global.window.onComponentDestroyed = onComponentDestroyed
//   global.window.onComponentCreated = onComponentCreated
// }
