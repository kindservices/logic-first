package kind.logic.telemetry

import kind.logic._
import kind.logic.color.ConsoleColors._

import concurrent.duration._
final case class CompletedCall(invocation: CallSite, response: CallResponse) {

  /** @return
    *   a call which represents the response from this call if this call has completed
    */
  def inverse: Option[CompletedCall] = {
    response match {
      case CallResponse.NotCompleted     => None
      case CallResponse.Error(ts, error) =>
        // we're representing the response from a call, so the "Response" from the response is 'NotCompleted'
        val inverse = copy(invocation = invocation.flip(error, ts), CallResponse.NotCompleted)
        Option(inverse)
      case CallResponse.Completed(ts, result) =>
        val inverse = copy(invocation = invocation.flip(result, ts), CallResponse.NotCompleted)
        Option(inverse)
    }
  }

  export invocation.*
  def atDateTime = java.time.Instant.ofEpochMilli(timestamp.asMillis)

  def endTimestamp = response.timestampOpt.ensuring {
    case None      => true
    case Some(end) => end.asNanos >= timestamp.asNanos
  }

  def durationFormatted: String = duration.fold("♾️") { nanos =>
    val millis = nanos.toNanos.toDouble / 1000000
    f"$millis%.4fms"
  }
  def duration: Option[FiniteDuration] = response match {
    case CallResponse.Error(e, _)     => Option((e.asNanos - timestamp.asNanos).nanos)
    case CallResponse.Completed(e, _) => Option((e.asNanos - timestamp.asNanos).nanos)
    case CallResponse.NotCompleted    => None
  }

  def resultText = response match {
    case CallResponse.Error(_, err)        => red(s"failed with $err")
    case CallResponse.Completed(_, result) => green(s"returned $result")
    case CallResponse.NotCompleted         => yellow("never completed")
  }
  override def toString = toStringColor

  def toStringMonocolor =
    s"$source --[ $operation ]--> $target $resultText and took $duration at $atDateTime"

  def operationArrow = s"--[ $operation ]-->"

  def toStringColor =
    s"${blue(source.toString)} ${purple(operationArrow)} ${yellow(target.toString)} $resultText and took $duration at $atDateTime"
}
