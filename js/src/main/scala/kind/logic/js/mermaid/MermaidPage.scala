package kind.logic.js.mermaid

import org.scalajs.dom
import org.scalajs.dom.{Node, document, html}
import scalatags.JsDom.all.*
import ujson.Value

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import kind.logic.js.TestScenario

@js.native
@JSGlobal("renderMermaid")
object RenderMermaid extends js.Object {
  def apply(targetElm: Node): Unit = js.native
}

case class MermaidPage() {

  private val titleDiv = div().render

  def update(scenario: TestScenario, newMarkdown: String) = {
    titleDiv.innerHTML = s"Mermaid diagram for ${scenario.name}"
    renderDiagram(newMarkdown)
  }

  /** We couldn't parse the scenario
    */
  def updateError(scenario: TestScenario, errorMessage: String) = {
    titleDiv.innerHTML = s"Failed to unmarshal the test data for ${scenario.name}"
    updateDiagramComment(errorMessage)
  }

  // Create a div to display the diagram
  private val diagramDiv = div(
    id        := "diagram",
    border    := "1px solid #ccc",
    padding   := "20px",
    marginTop := "20px"
  ).render

  // Append the elements to the body
  def element = div(style := "background-color: white", diagramDiv).render

  private def renderDiagram(markdown: String): Unit = {
    val newTargetNode = updateDiagramComment(markdown)
    RenderMermaid(newTargetNode)
  }

  private def updateDiagramComment(content: String) = {
    diagramDiv.innerHTML = ""

    val newTargetNode = dom.document.createElement("div")
    newTargetNode.setAttribute("id", "diagram")
    newTargetNode.textContent = content
    diagramDiv.appendChild(newTargetNode)
    newTargetNode
  }
}
