package kind.logic

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import kind.logic.*
import kind.logic.json.*
import kind.logic.dsl.*
import kind.logic.telemetry.Telemetry
import upickle.default.*
import zio.*

class DSLTest extends AnyWordSpec with Matchers {

  "the 'call' DSL method" should {
    "be able trace scala code blocks which return a Task using 'withArgs'" in {

      // for the 'call' DSL, we need telemetry and a source container in-scope
      given t: Telemetry    = Telemetry()
      given user: Container = Container.person("org", "user")

      locally {
        // the example under test
        val returnsATask = call(Container.service("app", "foo")).withArgs(123) {
          ZIO.attempt("I am a task")
        }

        returnsATask.execOrThrow() shouldBe "I am a task"
        val mermaid       = t.mermaid.execOrThrow()
        val Seq(onlyCall) = mermaid.calls

        onlyCall.input shouldBe 123
        onlyCall.source shouldBe user
        onlyCall.target shouldBe Container.service("app", "foo")
        onlyCall.operation shouldBe "returnsATask"
        onlyCall.response.asOption shouldBe Some("I am a task")
      }
    }
    "be able trace scala code blocks which return a Task" in {

      // for the 'call' DSL, we need telemetry and a source container in-scope
      given t: Telemetry    = Telemetry()
      given user: Container = Container.person("org", "user")

      locally {
        // the example under test
        val returnsATask = call(Container.service("app", "foo")) {
          ZIO.attempt("I am a task")
        }

        returnsATask.execOrThrow() shouldBe "I am a task"
        val mermaid       = t.mermaid.execOrThrow()
        val Seq(onlyCall) = mermaid.calls

        onlyCall.input shouldBe ()
        onlyCall.source shouldBe user
        onlyCall.target shouldBe Container.service("app", "foo")
        onlyCall.operation shouldBe "returnsATask"
        onlyCall.response.asOption shouldBe Some("I am a task")
      }
    }
    "be able trace normal scala code blocks" in {

      // for the 'call' DSL, we need telemetry and a source container in-scope
      given t: Telemetry    = Telemetry()
      given user: Container = Container.person("org", "user")

      locally {
        // the example under test
        val someExampleWithArgs = call(Container.service("app", "foo")).withArgs(1) {
          // some code
          3 * 4
        }

        someExampleWithArgs.execOrThrow() shouldBe 12
        val mermaid       = t.mermaid.execOrThrow()
        val Seq(onlyCall) = mermaid.calls

        onlyCall.input shouldBe 1
        onlyCall.source shouldBe user
        onlyCall.target shouldBe Container.service("app", "foo")
        onlyCall.operation shouldBe "someExampleWithArgs"
        onlyCall.response.asOption shouldBe Some(12)

      }

      locally {
        // and now without the '.withArgs'
        val someExampleWithoutArgs = call(Container.service("app", "foo")) {
          // some code
          456
        }
        someExampleWithoutArgs.execOrThrow() shouldBe 456
        val mermaid            = t.mermaid.execOrThrow()
        val Seq(_, secondCall) = mermaid.calls

        secondCall.source shouldBe user
        secondCall.target shouldBe Container.service("app", "foo")
        secondCall.operation shouldBe "someExampleWithoutArgs"
        secondCall.response.asOption shouldBe Some(456)
        secondCall.input shouldBe (())
      }
    }
  }

}
