package kind.logic.telemetry

import kind.logic.Timestamp

enum CallResponse:
  case NotCompleted
  case Error(timestamp: Timestamp, bang: Any)
  case Completed(timestamp: Timestamp, result: Any)
  def timestampOpt: Option[Timestamp] = this match {
    case NotCompleted     => None
    case Error(ts, _)     => Option(ts)
    case Completed(ts, _) => Option(ts)
  }
