package kind.logic.telemetry

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import kind.logic.*
import kind.logic.json.*
import upickle.default.*
import zio.*

class TelemetryTest extends AnyWordSpec with Matchers {

  case class Data(name : String) derives ReadWriter

  "Telemetry" should {
    "be able to produce valid mermaid" in {

      given underTest : Telemetry = Telemetry()

      val BFF = Actor.service("example", "BFF")
      val Server = Actor.service("example", "App")
      val DB = Actor.database("managedsvc", "Mongo")
      val Node = Actor.service("example", "ViewServer")


      val flow = for
        _ <- ZIO.succeed("ok").traceWith(Node, BFF, Data("foo").withKey("input").merge("onClick".withKey("useraction")))
        _ <- ZIO.succeed(1).traceWith(BFF, Server, Data("foo").withKey("data").merge("doSave".withKey("action")))
        _ <- ZIO.succeed("ok").traceWith(Server, DB, "foo".withKey("record").merge("persist".withKey("action")))
        _ <- ZIO.succeed(200).traceWith(Server, BFF, "123".withKey("id").asUJson)
        _ <- ZIO.succeed(200).traceWith(BFF, Node, "Good to go!".withKey("text/plain").asUJson)
      yield ()

      flow.asTry() // <--= run it

      val mermaid = underTest.asMermaidDiagram(maxLenComment = 120).execOrThrow()

      // you can test this output out at https://mermaid.live
      println("Test this at mermaid.live:")
      println(mermaid)
    }
  }
}
