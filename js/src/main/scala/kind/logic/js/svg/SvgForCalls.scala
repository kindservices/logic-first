package kind.logic.js.svg

import kind.logic._
import kind.logic.telemetry._
import org.scalajs.dom.HTMLDivElement

import scala.concurrent.duration._

object SvgForCalls {

  def adjustShortTimestamps(
      calls: Seq[SendMessage],
      lastTimestamp: Timestamp = 0.asTimestampNanos,
      minDuration: FiniteDuration = 5.millis
  ): Seq[SendMessage] = {
    calls match {
      case Seq() => Seq()
      case head +: theRest =>
        val newTimestamp =
          if head.timestamp.asNanos <= lastTimestamp.asNanos then
            (lastTimestamp.asNanos + minDuration.toNanos).asTimestampNanos
          else head.timestamp
        val newDuration = if head.duration < minDuration then minDuration else head.duration

        val newTime = head.copy(timestamp = newTimestamp, duration = newDuration)
        newTime +: adjustShortTimestamps(theRest, newTime.endTimestamp, minDuration)
    }
  }

  def apply(
      calls: Seq[CompletedCall],
      config: ui.Config = ui.Config.default(),
      desiredTimeRange: FiniteDuration = 10.seconds
  ): HTMLDivElement = {
    val messages = SendMessage.fromCalls(calls.sortBy(_.timestamp.asNanos))
    val adjusted = adjustShortTimestamps(messages)
    val scaled   = SendMessage.scaleToFit(adjusted, desiredTimeRange)

    val actors = calls.flatMap(c => Set(c.source, c.target))
    SvgComponent(actors, scaled, config)
  }

}
