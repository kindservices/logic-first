package kind.logic.telemetry

import kind.logic._
import zio._

/** Represents a Call invocation -- something we'd want to capture in our archtiecture (i.e. a
  * sequence diagram describing our system)
  *
  * @param input
  *   the input which triggered this call
  * @param response
  *   a holder for when the response completes
  */
private[telemetry] final class Call(
    invocation: CallSite,
    response: Ref[CallResponse]
) {

  export invocation.action.source
  export invocation.action.target

  override def toString = invocation.toString

  def asCompletedCall: UIO[CompletedCall] = {
    response.get.map { resp =>
      CompletedCall(invocation, resp)
    }
  }

  /** completeWith will always finish the calls, even on error
    *
    * @param result
    * @return
    *   a 'pimped' Task which will update the response ref when run
    */
  def completeWith[A](result: Task[A]): Task[A] = {
    for
      either <- result.either
      time   <- now
      result <- either match {
        case Left(err) =>
          for
            _      <- response.set(CallResponse.Error(time.asTimestampNanos, err))
            failed <- ZIO.fail(err)
          yield failed
        case Right(ok) =>
          response.set(CallResponse.Completed(time.asTimestampNanos, ok)).as(ok)
      }
    yield result
  }
}

object Call {
  def apply(action: Action, operation: Any): ZIO[Any, Nothing, Call] = {
    for
      time        <- now
      responseRef <- Ref.make[CallResponse](CallResponse.NotCompleted)
    yield new Call(CallSite(action, operation, time.asTimestampNanos), responseRef)
  }
}
