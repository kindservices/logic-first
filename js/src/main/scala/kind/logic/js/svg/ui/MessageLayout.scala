package kind.logic.js.svg.ui

import kind.logic.js.svg.*
import kind.logic.*
import kind.logic.telemetry.*
import kind.logic.color.Colors

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import org.scalajs.dom
import scalatags.JsDom
import scalatags.JsDom.svgTags as stags
import scalatags.JsDom.svgTags.*
import scalatags.JsDom.svgAttrs.*
import scalatags.JsDom.implicits.{*, given}

import scala.scalajs.js.annotation.*
import scala.concurrent.duration.{*, given}
import scala.collection.MapView
import scalatags.JsDom.TypedTag
import org.scalajs.dom.Element
import org.scalajs.dom.SVGCircleElement
import org.scalajs.dom.SVGTextElement

object MessageLayout:

  /** So that the parent can be notified when a message is clicked. The params are the event (so we
    * can show a tooltip at the mouse position), and an optional message (it's None if we've moved
    * away from the message, and Some(message) if we're still on it
    */
  type Callback = (dom.MouseEvent, Option[RenderedMessage]) => Unit

  case class RenderedMessage(
      label: String,
      message: SendMessage,
      at: Point,
      color: String,
      onClick: Callback
  ) {

    def msgFromString = s"${message.from.category}:${message.from.label}"
    def msgToString   = s"${message.to.category}:${message.to.label}"
    export message.{isActiveAt, timestamp, from, to, duration}

    val asSvg: Element = g(
      circle(
        id          := s"message-$label",
        cx          := at.x,
        cy          := at.y,
        r           := 25,
        stroke      := color,
        strokeWidth := 4,
        fill        := color
      ),
      text(
        textAnchor       := "middle",
        dominantBaseline := "middle",
        fontSize         := 30,
        transform        := s"translate(${at.x},${at.y})"
      )(label)
    ).render

    /** Move the message to a new point.
      *
      * @param point
      *   the new point
      * @param percentageOnJourney
      *   the percentage (0.0 to 1.0) on the path from the start to the end
      */
    def moveTo(point: Point, percentageOnJourney: Double) = {
      val clip: Double = 0.05
      val opacity      = OpacityCurve(percentageOnJourney, clip).toString
      def callback(hovering: Boolean) = (e: dom.MouseEvent) => {
        val payload = Option(this).filter(_ => hovering)
        if (percentageOnJourney > clip && percentageOnJourney < 1 - clip) then onClick(e, payload)
      }

      asSvg.childNodes.foreach { node =>
        node match {
          case value: SVGCircleElement =>
            value.setAttribute("cx", point.x.toString)
            value.setAttribute("cy", point.y.toString)
            value.setAttribute("opacity", opacity)
          // value.onmouseover = callback(true)
          // value.onmouseout = callback(false)
          case value: SVGTextElement =>
            value.setAttribute("transform", s"translate(${point.x},${point.y})")
            value.setAttribute("opacity", opacity)
            value.onmouseover = callback(true)
            value.onmouseout = callback(false)
          case nope =>
            sys.error(
              s"""If you see this, there is a bug in ${sourcecode.File}:${sourcecode.Enclosing} around line ${sourcecode.Line}. 
              We expected a circle or text, but got: $nope.
              Somebody updated the svg graph for messages, but forgot to update this 'moveTo' code.
              You're welcome. :)
              """
            )
        }
      }
    }
  }
end MessageLayout

class MessageLayout(
    allMessages: Seq[SendMessage],
    positionByActor: Map[Actor, Point],
    callback: MessageLayout.Callback
):
  val minTime: Timestamp = allMessages.map(_.timestamp.asNanos).min.asTimestampNanos
  val maxTime: Timestamp = allMessages.map(_.endTimestamp.asNanos).max.asTimestampNanos

  import MessageLayout.*

  val allMessagesWithId =
    allMessages.zipWithIndex.zip(Colors(allMessages.size, 60, 100)).map {
      case ((msg, idx), color) =>
        val id = ('A' + idx).toChar.toString
        // start all messages off-screen
        RenderedMessage(id, msg, Point(-1000, -1000), color, callback)
    }

  def elements = allMessagesWithId.map(_.asSvg)

  def tween(from: Point, to: Point, percentage: Double) = {
    val x = from.x + (to.x - from.x) * percentage
    val y = from.y + (to.y - from.y) * percentage
    Point(x.toInt, y.toInt)
  }

  def layout(atTime: Timestamp) = {
    allMessagesWithId.foreach { msg =>

      val fromPoint = positionByActor(msg.from)
      val toPoint   = positionByActor(msg.to)

      if !msg.isActiveAt(atTime) then {
        // there's a bug where if we don't reset the non-active messages and the user scrolls time really quickly,
        // those messages are left orphaned on the screen. This is an approach which fixes that by resetting them to their
        // base location. It works because the 'moveTo' logic will make messages transparent if they're at their start (or end) locations
        msg.moveTo(fromPoint, 0.0)
      } else {
        val percentage = (atTime.asNanos.toDouble - msg.timestamp.asNanos) / msg.duration.toMillis
        val point      = tween(fromPoint, toPoint, percentage)
        msg.moveTo(point, percentage)
      }
    }
  }
end MessageLayout
