package kind.examples.simple

import scala.language.implicitConversions
import kind.logic.json.*
import kind.logic.*
import kind.logic.telemetry.Telemetry
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.*

class App extends AnyWordSpec with Matchers {

  object nested {

    object web3 {
      trait Blockchain {
        import Blockchain.*
        def publish(smartContract: SmartContract): Task[ContractId]
      }
      object Blockchain {
        type SmartContract = String
        type ContractId    = String
        given System: Container = Container.service
        val Chain: Container    = Container.database.withName("besu")

        def apply()(using t: Telemetry): Blockchain = new Blockchain {
          def publish(smartContract: SmartContract): Task[ContractId] = {
            val id: ContractId = smartContract.hashCode().toString
            id.asTask.traceWith(Action.calls(Chain), smartContract)
          }
        }
      }
    }
    // this object name will be used as the 'software system'
    object contracts {
      type Spreadsheet   = Json
      type ApplicationId = String
      type ContractId    = String

      val testSpreadsheet = """{ "a" : "b" }""".parseAsJson

      case class Asset(name: String, data: Json)

      trait Onboarding {
        def create(spreadsheet: Spreadsheet): Task[ApplicationId]

        def list(): Task[Map[ApplicationId, Spreadsheet]]

        def publish(id: ApplicationId): Task[ContractId]
      }

      object Onboarding {
        // this name will be used as the 'component name'. The enclosing object 'assets' will be the 'software system'
        given System: Container = Container.service
        val Database            = Container.database.withName("DB")

        def apply(chain: web3.Blockchain)(using t: Telemetry): Onboarding = new Onboarding {
          def create(spreadsheet: Spreadsheet): Task[ApplicationId] =
            "id".asTask.traceWith(Action.calls(Database), spreadsheet)

          def list(): Task[Map[ApplicationId, Spreadsheet]] =
            Map("1" -> testSpreadsheet).asTask.traceWith(Action.calls(Database))

          override def publish(id: ApplicationId): Task[ContractId] = {
            val contract = s"smart contract for $id"
            for
              assetID <- chain
                .publish(contract)
                .traceWith(
                  Action.calls(web3.Blockchain.System, "publishToChain"),
                  id
                )
              _ <- s"db:id:${id}".asTask
                .traceWith(Action.calls(Database, "writeToDatabase"), assetID)
            yield id
          }
        }
      }
    }
  }
  import nested.*

  "Example Web3 App" should {
    "create a sequence diagram" in {
      given t: Telemetry                   = Telemetry()
      val dlt                              = web3.Blockchain()
      val onboarding: contracts.Onboarding = contracts.Onboarding(dlt)

      val testCase = for
        applicationId <- onboarding.create(contracts.testSpreadsheet)
        assetId       <- onboarding.publish(applicationId)
        mermaid       <- t.mermaid
        c4            <- t.c4
      yield (mermaid.diagram(), c4.diagram())

      val (mermaidDiagram, c4) = testCase.execOrThrow()
      import eie.io.{*, given}
      println(t.pretty)
      "sequence.md".asPath.text = mermaidDiagram
      "workspace.dsl".asPath.text = c4
    }
  }
}
