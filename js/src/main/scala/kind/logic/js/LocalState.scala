package kind.logic.js

import org.scalajs.dom
import scala.util.control.NonFatal
import concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** Contains stuff we need for the UI
  */
object LocalState {

  // the things we can store in local storage
  private enum Keys:
    case Scenarios
    def name = toString

  private def get(key: String): Option[String] = Option(dom.window.localStorage.getItem(key))
  private def set(key: String, value: String)  = dom.window.localStorage.setItem(key, value)

  // TODO - this coupling kinda sucks. Move this to a single data registry
  // private val happyPath = Map("Happy Path Draft Contract" -> TestScenario.happyPathDraftContract)

  //
  // TODO = there's a better way to do this, but we allow our top-level app to update this
  var defaultPaths: Map[String, TestScenario] = Map.empty

  private def loadScenariosByName(): Map[String, TestScenario] = {
    try {
      get(Keys.Scenarios.name)
        .map(s => ujson.read(s))
        .map(TestScenario.mapFromJson)
        .getOrElse(Map.empty)
    } catch {
      case NonFatal(e) =>
        println(s"Error reading scenarios from local storage: $e")
        Map.empty
    }
  }

  private var cachedScenarios: Map[String, TestScenario] = loadScenariosByName()

  // always include the happy path ... we can't ever delete that
  def scenariosByName: Map[String, TestScenario] = cachedScenarios ++ defaultPaths

  def removeScenario(name: String) = saveScenarios(scenariosByName - name)

  def addScenario(scenario: TestScenario): Map[String, TestScenario] = saveScenarios {
    scenariosByName.updated(scenario.name, scenario)
  }

  def saveScenarios(scenarios: Map[String, TestScenario]): Map[String, TestScenario] = {
    cachedScenarios = scenarios
    Future {
      val jason = ujson.write(scenarios.view.mapValues(_.asJson).toMap)
      set(Keys.Scenarios.name, jason)
    }
    scenarios
  }

}
