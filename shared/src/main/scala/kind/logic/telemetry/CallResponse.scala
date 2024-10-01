package kind.logic.telemetry

import kind.logic.Timestamp

enum CallResponse:
  case NotCompleted
  case Error(operationId: Long, timestamp: Timestamp, bang: Any)
  case Completed(operationId: Long, timestamp: Timestamp, result: Any)
  def responseId: Option[Long] = this match {
    case NotCompleted        => None
    case Error(id, _, _)     => Option(id)
    case Completed(id, _, _) => Option(id)
  }
  def timestampOpt: Option[Timestamp] = this match {
    case NotCompleted        => None
    case Error(_, ts, _)     => Option(ts)
    case Completed(_, ts, _) => Option(ts)
  }
