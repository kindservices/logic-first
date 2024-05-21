package kind.logic.telemetry

import kind.logic._

private[telemetry] case class CallSite(
    source: Actor,
    target: Actor,
    operation: Any,
    timestamp: Timestamp
)
