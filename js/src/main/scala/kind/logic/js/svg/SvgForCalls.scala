package kind.logic.js.svg
import kind.logic.telemetry.*

object SvgForCalls {

  def apply(calls: Seq[CompletedCall]): Seq[SendMessage] = {
    SendMessage.fromCalls(calls.sortBy(_.timestamp))
  }

}
