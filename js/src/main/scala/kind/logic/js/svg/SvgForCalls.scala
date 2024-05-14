package kind.logic.js.svg
import kind.logic.telemetry.*
import org.scalajs.dom.HTMLDivElement
import scala.concurrent.duration.*

object SvgForCalls {

  def apply(
      calls: Seq[CompletedCall],
      config: ui.Config = ui.Config.default(),
      desiredTimeRange: FiniteDuration = 10.seconds
  ): HTMLDivElement = {
    val messages = SendMessage.fromCalls(calls.sortBy(_.timestamp))
    val scaled   = SendMessage.scaleToFit(messages, desiredTimeRange)

    val actors = calls.flatMap(c => Set(c.source, c.target))
    SvgComponent(actors, scaled, config)
  }

}
