package kind.logic.js.svg

import kind.logic.telemetry.*
import kind.logic.js.svg.*
import kind.logic.js.svg.ui.*
import kind.logic.Actor
import org.scalajs.dom
import scalatags.JsDom.all.*
import scalatags.JsDom.implicits.given

import scala.concurrent.duration.{*, given}

object InteractiveComponent {
  def apply(actors: Seq[Actor], messages: Seq[SendMessage], config: Config = Config.default()) = {
    val systemActorLayout = SystemActorsLayout(actors.toSet, config)

    val tooltip = div(
      id := "tooltip",
      style := "display: none; position: absolute; width:500px; height: 500px; background: black; border: 1px solid black; margin: 5px; padding: 5px; text-align:left"
    ).render
    def onClick(e: dom.MouseEvent, messageOpt: Option[MessageLayout.RenderedMessage]) =
      messageOpt match {
        case Some(msg) =>
          // TODO: the message flashes/jumps if the div goes off the screen and introduces scrollbars
          tooltip.style.left = s"${e.clientX + 10}px"
          tooltip.style.top = s"${e.clientY + 10}px"
          tooltip.style.display = "flex"
          tooltip.innerHTML = ""

          def line(label: String, value: String) = div(
            scalatags.JsDom.all
              .span(style := "width: 50em; display: inline-block; font-weight: bold")(label),
            scalatags.JsDom.all.span(style := "display: inline-block;")(value)
          )

          tooltip.append(
            div(style := "text-align:left")(
              h2("Message"),
              line("From:", msg.msgFromString),
              line("To:", msg.msgToString),
              line("At:", s"${msg.timestamp}"),
              line("Took:", s"${msg.duration.toMillis}ms"),
              h3("Message:"),
              p(msg.message.messageFormatted)
            ).render
          )

        case None =>
          tooltip.style.display = "none"
      }

    val messagesLayout = MessageLayout(messages, systemActorLayout.positionByActor, onClick)

    div(
      tooltip,
      InteractiveMessageSvgComponent(systemActorLayout, messagesLayout, config).render
    ).render
  }
}
