package kind.logic.telemetry

import concurrent.duration.*
import kind.logic.color.ConsoleColors.*
final case class CompletedCall(invocation: CallSite, response: CallResponse) {

  export invocation.*
  def atDateTime = java.time.Instant.ofEpochMilli(timestamp)

  def endTimestamp = response.timestampOpt.ensuring {
    case None      => true
    case Some(end) => end >= timestamp
  }

  def durationFormatted: String = duration.fold("♾️") { nanos =>
    val millis = nanos.toNanos.toDouble / 1000000
    f"$millis%.4fms"
  }
  def duration: Option[FiniteDuration] = response match {
    case CallResponse.Error(end, _)     => Option((end - timestamp).nanos)
    case CallResponse.Completed(end, _) => Option((end - timestamp).nanos)
    case CallResponse.NotCompleted      => None
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
