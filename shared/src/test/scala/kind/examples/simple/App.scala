package kind.examples.simple

import kind.logic.telemetry.Telemetry
import kind.logic.{*, given}
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

      val testSpreadsheet = """{ "a" : "b" }""".asUJson

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
        val System   = Container.service
        val Database = System.withType(ContainerType.Database)

        def apply()(using t: Telemetry): Onboarding = new Onboarding {
          def create(spreadsheet: Spreadsheet): Task[ApplicationId] =
            "id".asTaskTraced(System, Database, spreadsheet)

          def list(): Task[Map[ApplicationId, Spreadsheet]] =
            Map("1" -> testSpreadsheet).asTaskTraced(System, Database)

          override def publish(id: ApplicationId): Task[AssetId] = {
            val method = implicitly[sourcecode.Enclosing].value
            val action = method.asAction.withInput(id)
            for
              assetID <- "publish-id".asTaskTraced(System, Dlt.System, action)
              _       <- "db-id".asTaskTraced(System, Database, assetID)
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
      yield diagram

      val mermaid = testCase.execOrThrow()
      import eie.io.{given, *}
      println("onboarding.md".asPath.text = mermaid)

    }
  }
}
