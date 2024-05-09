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

// def interactiveAsRestaurant(scenario: TestScenario): Option[HTMLDivElement] = {
//   try {
//     val request = read[OrderData](scenario.input)

//     given telemetry: Telemetry = Telemetry()

//     val result = RestaurantDefaultLogic.newRestaurant.placeOrder(request.asOrder).execOrThrow()
//     val calls: Seq[CompletedCall] = telemetry.calls.execOrThrow()
//     val messages                  = kind.logic.js.svg.SvgForCalls(calls)
//     // TODO - scale the config based on the div size
//     val actors = calls.flatMap(c => Set(c.source, c.target))
//     val config = ui.Config.default()
//     Option(InteractiveComponent(actors, messages, config))

//   } catch {
//     case NonFatal(e) =>
//       None
//   }
// }

// lazy val interactivePage = {
//   val page: HTMLDivElement = div().render

//   EventBus.activeTestScenario.subscribe { scenario =>
//     val tryOne = interactiveAsDraftContract(scenario)

//     val tryTwo = interactiveAsRestaurant(scenario)

//     val fallback = div(s"We couldn't parse the scenario as a DraftContract or Restaurant").render

//     val component = tryOne.orElse(tryTwo).getOrElse(fallback)

//     page.innerHTML = ""
//     page.appendChild(component)

//   }
//   page
// }

@JSExportTopLevel("createScenarioBuilder")
def createScenarioBuilder(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
  container.replace(ScenarioBuilder().content)

@JSExportTopLevel("createSequenceDiagram")
def createSequenceDiagram(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
  container.replace(AppSkeleton.mermaidPage.element)

@JSExportTopLevel("createInteractivePage")
def createInteractivePage(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
  container.replace(AppSkeleton.svgPage)

@JSExportTopLevel("createDiffPage")
def createDiffPage(container: scala.scalajs.js.Dynamic, state: scala.scalajs.js.Dynamic) =
  container.placeholder("Diff", state)

// this is used to update the menu
@JSExportTopLevel("onComponentCreated")
def onComponentCreated(id: String) = UIComponent.byFunction(id).foreach(EventBus.tabOpen.publish)

@JSExportTopLevel("onComponentDestroyed")
def onComponentDestroyed(id: String) =
  UIComponent.byFunction(id).foreach(EventBus.tabClosed.publish)

@main
def layout(): Unit = {
  new Drawer(HtmlUtils.$("drawer")).refresh()

  global.window.createScenarioBuilder = createScenarioBuilder
  global.window.createSequenceDiagram = createSequenceDiagram
  global.window.createInteractivePage = createInteractivePage
  global.window.createDiffPage = createDiffPage

  global.window.onComponentDestroyed = onComponentDestroyed
  global.window.onComponentCreated = onComponentCreated
}
