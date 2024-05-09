package kind.logic.telemetry

enum CallResponse:
  case NotCompleted
  case Error(timestamp: Long, bang: Any)
  case Completed(timestamp: Long, result: Any)
  def timestampOpt: Option[Long] = this match {
    case NotCompleted     => None
    case Error(ts, _)     => Option(ts)
    case Completed(ts, _) => Option(ts)
  }
