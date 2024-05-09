package kind.logic.telemetry

import kind.logic.*

private[telemetry] case class CallSite(
    source: Actor,
    target: Actor,
    operation: Any,
    timestamp: Long
)
