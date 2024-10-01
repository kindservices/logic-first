package kind.examples.simple

import scala.language.implicitConversions
import kind.logic.json.*
import kind.logic.*
import kind.logic.telemetry.Telemetry
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.*

/** This is a basic example, written as a test-case.
  *
  * It contains the components (Containers) for an n-tier app consisting of some users, a UI, a
  * backend-for-frontend, service and database.
  */
class App extends AnyWordSpec with Matchers {

  given UI: Container       = Container.webApp("App", "UI")
  given Service: Container  = Container.service("App", "Backend")
  given Database: Container = Container.database("App", "PostgreSQL")

  given Admin: Container = Container.person("Acme", "Admin")

  // this is an example of just a naked function - some operation we annotate w/ our service
  def saveData(data: String)(using telemetry: Telemetry) = {
    given System: Container = Service
    data.hashCode().asTask.traceWith(Action.calls(Database), data)
  }

  // this is an example of a basic object (not a trait) where we can just put some functions
  object BFF {
    given System: Container = Container.service
    def save(data: String)(using telemetry: Telemetry) =
      saveData(data).traceWith(Action.calls(Service), data)
  }

  // this is a more typical example of defining an interface for a service with a companion object
  trait Search {
    def query(term: String): Task[Seq[Search.Result]]
  }
  object Search {
    // just some made-up type. Here we have a map of words to their length
    type Result = Map[String, Int]

    // this could be a micro-service, but here we'll make search the responsibility of the same B/E service
    given System: Container = Service

    def apply()(using telemetry: Telemetry): Search = new Search {
      override def query(term: String): Task[Seq[Search.Result]] = {
        val parts = term.split(" ").toSeq
        for result <- ZIO.foreachPar(parts) { word =>
            Map(word -> word.length).asTask.traceWith(Action.calls(Database), word)
          }
        yield result
      }
    }
  }

  "Example App" should {
    "create a sequence diagram and c4 diagram from a basic app" in {
      given t: Telemetry = Telemetry()

      // here is a basic, typical flow where somebody saves some data and then does a query
      val data = "some input"
      val testCase = for
        _ <- (data.asTask *> BFF.save(data).traceWith(Action(UI, BFF.System, "onSave"), data))
          .traceWith(Action(Admin, UI, "save form"), data)
        queryApi    = Search()
        queryString = "the quick brown fox"
        userQuery <- (queryString.asTask *> queryApi
          .query(queryString)
          .traceWith(Action(UI, Service, "query"), queryString))
          .traceWith(Action(Admin, UI, "do a search"), queryString)
        mermaid <- t.mermaid
        c4      <- t.c4
      yield (mermaid.diagram(), c4.diagram())

      val (mermaidDiagram, c4) = testCase.execOrThrow()
      import eie.io.{*, given}
      println(t.pretty)
      "sequence.md".asPath.text = mermaidDiagram
      "workspace.dsl".asPath.text = c4
    }
  }
}
