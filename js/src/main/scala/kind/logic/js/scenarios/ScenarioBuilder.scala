package kind.logic.js.scenarios

import kind.logic.js._
import org.scalajs.dom.MouseEvent
import scalatags.JsDom.all._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.chaining._
case class ScenarioBuilder() {

  private def onDelete() = {
    LocalState.removeScenario(selectInput.value)
    refreshSelect()
    notifyOnSelectChanged()

    currentScenario().foreach(updateContent)
  }

  private val deleteButton =
    button(
      cls   := "btn",
      style := "margin-left: 20em",
      id    := "delete-button",
      "❌ Delete Scenario"
    ).render.tap(_.onclick = _ => onDelete())
  private def choices = LocalState.scenariosByName.keys.toSeq.sorted.map { name =>
    option(value := name, name).render
  }

  private val selectInput = select(
    id   := "scenario",
    name := "scenario",
    choices
  ).render
  private val selectInputDiv = div(
    label(
      cls := "label",
      "Scenario Name:",
      `for` := "scenario"
    ),
    selectInput,
    deleteButton
  )

  // our previously selected scenario
  private var lastSelected = selectInput.value

  private def refreshSelect(): Unit = {
    selectInput.innerHTML = ""
    var lastSelectedIsPresent = false
    choices.foreach { opt =>
      lastSelectedIsPresent = lastSelectedIsPresent || (opt.value == lastSelected)
      selectInput.appendChild(opt)
    }

    // when we delete a scenario, we want to keep the last selected scenario
    if lastSelectedIsPresent then selectInput.value = lastSelected
    else notifyOnSelectChanged()

  }

  private def currentScenario(): Option[TestScenario] = LocalState.scenariosByName.get(lastSelected)

  private def notifyOnSelectChanged() = {
    if lastSelected != selectInput.value then {
      println(s"changing lastSelected from $lastSelected to ${selectInput.value} ")
      lastSelected = selectInput.value
      currentScenario().foreach(EventBus.activeTestScenario.publish)
    }
  }

  private val testButton =
    button(cls := "btn", id := "test-button", "Test").render.tap(_.onclick = _ => onTest())

  private val saveAsButton = button(cls := "btn", id := "save-button", "Save As").render
    .tap(_.onclick = _ => openModal())

  private def closeModal(): Unit = {
    modalForm.classList.remove("modal-show")
    modalForm.classList.add("modal")
  }

  private def openModal(): Unit = {
    // clear the field
    nameInput.value = ""
    modalForm.classList.remove("modal")
    modalForm.classList.add("modal-show")
    nameInput.focus()
  }

  // here we just want to publish what's in the current json input w/o saving
  private def onTest() = {
    EventBus.activeTestScenario.publish(currentScenarioFromInput())
  }

  def currentScenarioFromInput(): TestScenario =
    TestScenario(nameInput.value, descriptionInput.value, jsonInput.value.asUJson)

  private def onSaveAsOk() = {
    jsonInput.value

    val scenario = currentScenarioFromInput()
    // no validation! naughty...
    LocalState.addScenario(scenario)
    lastSelected = scenario.name
    refreshSelect()
    notifyOnSelectChanged()
    closeModal()
  }

  private val nameInput =
    input(`type` := "text", style := "margin: 10px; width: 300px", id := "sceanrioNameInput").render

  private val descriptionInput =
    textarea(
      id    := "description-text-area",
      cls   := "big-input",
      style := "margin: 10px; width: 400px",
      rows  := 4,
      label("Description")
    ).render

  private val cancelSave = button(`type` := "button", cls := "btn", "Cancel").render
    .tap(_.onclick = (e: MouseEvent) => closeModal())

  private val saveTestCase = button(`type` := "button", cls := "btn", "Save").render.tap {
    _.onclick = (e: MouseEvent) => {
      onSaveAsOk()
    }
  }

  private val modalForm = div(
    id  := "modalForm",
    cls := "modal",
    div(
      cls := "modal-content",
      div(label(`for` := nameInput.id, "Scenario:"), nameInput),
      div(label(`for` := descriptionInput.id, "Description:")),
      div(descriptionInput),
      cancelSave,
      saveTestCase
    )
  ).render

  private val jsonInput = textarea(
    id   := "input-text-area",
    cls  := "big-input",
    rows := 40,
    label("Input Text")
  ).render

  private def updateContent(scenario: TestScenario): Unit = {
    selectInput.value = scenario.name
    jsonInput.value = scenario.input.render(2)
  }

  val content = {
    div(
      selectInputDiv,
      div(jsonInput),
      div(testButton, saveAsButton),
      modalForm
    ).render
  }

  // set our initial content
  currentScenario().foreach(updateContent)

  selectInput.onchange = _ => {
    println("select changed")
    notifyOnSelectChanged()
    currentScenario().foreach(updateContent)
    // updateContent(scenario)
    refreshSelect()
  }

  // send an initial event after load
  Future {
    currentScenario().foreach(EventBus.activeTestScenario.publish)
  }
}
