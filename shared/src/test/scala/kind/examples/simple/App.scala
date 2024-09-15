package kind.examples.simple

import kind.logic.telemetry.Telemetry
import kind.logic.{*, given}
import kind.logic.json.{*, given}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.*

class App extends AnyWordSpec with Matchers {

  object nested {

    // this object name will be used as the 'software system'
    object assets {
      type Spreadsheet   = Json
      type ApplicationId = String
      type AssetId       = String

      val testSpreadsheet = """{ "a" : "b" }""".parseAsJson

      case class Asset(name: String, data: Json)

      trait Dlt {}
      object Dlt {
        val System = Container.service
      }

      trait Onboarding {
        def create(spreadsheet: Spreadsheet): Task[ApplicationId]

        def list(): Task[Map[ApplicationId, Spreadsheet]]

        def publish(id: ApplicationId): Task[AssetId]
      }

      object Onboarding {
        // this name will be used as the 'component name'. The enclosing object 'assets' will be the 'software system'
        given System: Container = Container.service
        val Database            = Container.database.withName("DB")

        def apply()(using t: Telemetry): Onboarding = new Onboarding {
          def create(spreadsheet: Spreadsheet): Task[ApplicationId] =
            "id".asTask.traceWith(Action.calls(Database), spreadsheet)

          def list(): Task[Map[ApplicationId, Spreadsheet]] =
            Map("1" -> testSpreadsheet).asTask.traceWith(Action.calls(Database))

          override def publish(id: ApplicationId): Task[AssetId] = {
            for
              assetID <- "publish-id".asTask.traceWith(
                Action.calls(Dlt.System, "publishToChain"),
                id
              )
              _ <- "db-id".asTask.traceWith(Action.calls(Database, "writeToDatabase"), assetID)
            yield id
          }
        }
      }

      trait Definition {
        def list(): Task[Map[AssetId, Asset]]

        def get(id: AssetId): Task[Asset]
      }
    }

    object uim {
      type UserId = String
    }

    object marketplace {
      trait BulletinBoard {
        def query(filter: String): Task[Seq[assets.Asset]]
      }

      case class ExecutionReport(
          buyer: uim.UserId,
          seller: uim.UserId,
          asset: assets.AssetId,
          quantity: Int
      )
      trait Exchange {
        def trade(trade: ExecutionReport): Task[Unit]
      }
    }
  }
  import nested.*

  "App.onboarding" should {
    "create a sequence diagram" in {
      given t: Telemetry                = Telemetry()
      val onboarding: assets.Onboarding = assets.Onboarding()

      val testCase = for
        applicationId <- onboarding.create(assets.testSpreadsheet)
        assetId       <- onboarding.publish(applicationId)
        diagram       <- t.mermaid.diagram()
        c4       = t.c4.diagram
      yield (diagram, c4)

      val (mermaidDiagram, c4) = testCase.execOrThrow()
      import eie.io.{given, *}
      println(t.pretty)
      println("-" * 80)
      println(t.mermaid.callStack.execOrThrow().mkString("\n"))
      println("-" * 80)
      println("onboarding.md".asPath.text = mermaidDiagram)

      println("workspace.dsl".asPath.text = c4)
    }
  }
}
