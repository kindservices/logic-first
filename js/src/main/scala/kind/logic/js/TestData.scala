package kind.logic.js

import kind.logic.Actor
import kind.logic.telemetry.*
import kind.logic.js.svg.*
import kind.logic.js.svg.ui.*
import org.scalajs.dom
import scalatags.JsDom.all.*
import scalatags.JsDom.implicits.given

import scala.concurrent.duration.{*, given}

object TestData {
  val actors: List[Actor] = List(
    // Actor.person("counterparty-A", "Alice"),
    // Actor.person("counterparty-A", "Alice's Lawyer"),
    // Actor.database("counterparty-service", "MongoDB"),
    // Actor.queue("counterparty-service", "Notification Queue"),
    // Actor.service("counterparty-service", "Counterparty Service"),
    // Actor.service("counterparty-service", "Notification ETL"),
    // Actor.email("counterparty-service", "Email Service"),
    // Actor.person("counterparty-B", "Bob")
  )
  val List(alice, lawyer, mongo, queue, service, etl, email, bob) = actors
  // val List(alice, bob) = actors
  val messages: List[SendMessage] = List(
    // SendMessage(alice, service, 1234, 1.seconds, ujson.Obj("hello" -> "world")),
    // SendMessage(alice, bob, 1234, 1.seconds, ujson.Obj("hello" -> "world")),
    // SendMessage(service, mongo, 2345, 2.seconds, ujson.Obj("hello" -> "world")),
    // SendMessage(alice, service, 1234, 10.seconds, ujson.Obj("go" -> "there"))
  )

  val config = Config.default()

  def interactive = InteractiveComponent(actors, messages, config).render
}
