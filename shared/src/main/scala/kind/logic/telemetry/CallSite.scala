package kind.logic.telemetry

import kind.logic._

private[telemetry] case class CallSite(
    source: Container,
    target: Container,
    operation: Any,
    timestamp: Timestamp
) {
  def flip(newOperation: Any, newTimestamp: Timestamp): CallSite = {
    copy(source = target, target = source, operation = newOperation, timestamp = newTimestamp)
  }
}
