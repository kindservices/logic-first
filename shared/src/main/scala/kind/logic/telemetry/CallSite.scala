package kind.logic.telemetry

import kind.logic._

private[telemetry] case class CallSite(
    action: Action,
    input: Any,
    timestamp: Timestamp
) {
  export action.*
}
