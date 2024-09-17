package kind.logic.telemetry

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import kind.logic.*
import kind.logic.json.*
import upickle.default.*
import zio.*

class TelemetryTest extends AnyWordSpec with Matchers {

  case class Data(name: String) derives ReadWriter

  def someService(foo: String)(using t: Telemetry): Task[String] = {
    given from: Container = Container.service("test", "from")
    val to                = Container.service("test", "to")

    zio.ZIO
      .succeed(s"result of calling someService('${foo}')")
      .traceWith(Action.calls(to), foo)(using t)
  }

  "Telemetry.mermaid.diagram" should {
    "work for a single call" in {
      val t = Telemetry()
      someService("example")(using t).execOrThrow()
      val mermaid = t.mermaid.execOrThrow().asMermaid(maxLenComment = 500, maxComment = 500)

      mermaid should include("test.from ->> test.to : someService(example)")
      mermaid should include(
        "test.to -->> test.from : result of calling someService('example')"
      )
    }
    "be able to produce valid mermaid" in {

      given underTest: Telemetry = Telemetry()

      val BFF    = Container.service("example", "BFF")
      val Server = Container.service("example", "App")
      val DB     = Container.database("managedsvc", "Mongo")
      val Node   = Container.service("example", "ViewServer")

      val flow = for
        _ <- ZIO
          .succeed("ok")
          .traceWith(
            Action(Node, BFF, "action"),
            Data("foo").withKey("input").merge("onClick".withKey("useraction"))
          )
        _ <- ZIO
          .succeed(1)
          .traceWith(Action(BFF, Server, "doSave"), Data("foo").withKey("data"))
        _ <- ZIO
          .succeed("ok")
          .traceWith(Action(Server, DB, "persist"), "foo".withKey("record"))
        _ <- ZIO.succeed(200).traceWith(Action(Server, BFF, "save"), "123".withKey("id").asUJson)
        _ <- ZIO
          .succeed(200)
          .traceWith(Action(BFF, Node, "response"), "Good to go!".withKey("text/plain").asUJson)
      yield ()

      flow.asTry() // <--= run it

      val mermaid = underTest.mermaid.execOrThrow().diagram(maxLenComment = 120)

      // you can test this output out at https://mermaid.live
      println("Test this at mermaid.live:")
      println(mermaid)
    }
  }
}
