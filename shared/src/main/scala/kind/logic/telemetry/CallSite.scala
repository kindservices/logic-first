package kind.logic.telemetry

import kind.logic._

private[telemetry] case class CallSite(
    action: Action,
    input: Any,
    timestamp: Timestamp
) {
  export action.*
  def flip(newInput: Any, newTimestamp: Timestamp): CallSite = {
    copy(
      action = action.flip(s"${action.operation} response"),
      input = newInput,
      timestamp = newTimestamp
    )
  }
}
