package kind.logic.telemetry

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import kind.logic.*
import kind.logic.json.*
import upickle.default.*
import zio.*

class TelemetryTest extends AnyWordSpec with Matchers {

  case class Data(name: String) derives ReadWriter

  def someService(foo : String)(using t : Telemetry) : Task[String] = {
    val from = Actor.service("test", "from")
    val to = Actor.service("test", "to")

    val command = ujson.Str("someService").withKey("action").mergeWith(foo.withKey("foo"))
    zio.ZIO.succeed(s"result of calling someService('${foo}')").traceWith(from, to, command)(using t)
  }

  "Telemetry.asMermaidDiagram" should {
    "work for a single call" in {
      val t = Telemetry()
      someService("example")(using t).execOrThrow()
      val mermaid = t.asMermaid(maxLenComment = 500, maxComment = 500).execOrThrow()

      mermaid should include("test.from ->> test.to : someService {\"foo\": \"example\"}")
      mermaid should include("test.to -->> test.from : {\"foo\": \"example\"} Returned 'result of calling someService('example')'")
    }
    "be able to produce valid mermaid" in {

      given underTest: Telemetry = Telemetry()

      val BFF    = Actor.service("example", "BFF")
      val Server = Actor.service("example", "App")
      val DB     = Actor.database("managedsvc", "Mongo")
      val Node   = Actor.service("example", "ViewServer")

      val flow = for
        _ <- ZIO
          .succeed("ok")
          .traceWith(Node, BFF, Data("foo").withKey("input").merge("onClick".withKey("useraction")))
        _ <- ZIO
          .succeed(1)
          .traceWith(BFF, Server, Data("foo").withKey("data").merge("doSave".withKey("action")))
        _ <- ZIO
          .succeed("ok")
          .traceWith(Server, DB, "foo".withKey("record").merge("persist".withKey("action")))
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
