package kind.logic.telemetry

import kind.logic.Timestamp

enum CallResponse:
  case NotCompleted
  case Error(operationId: Long, timestamp: Timestamp, bang: Any)
  case Completed(operationId: Long, timestamp: Timestamp, result: Any)

  def asTry =this match {
    case NotCompleted        => scala.util.Failure(new Exception("operation did not complete"))
    case Error(_, _, bang: Throwable)     => scala.util.Failure(bang)
    case Error(_, _, bang)     => scala.util.Failure(new Exception(bang.toString))
    case Completed(_, _, result) => scala.util.Success(result)
  }

  def asOption = asTry.toOption

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
