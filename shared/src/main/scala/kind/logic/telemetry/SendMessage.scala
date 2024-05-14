package kind.logic.telemetry

import kind.logic._

import scala.concurrent.duration.Duration
import scala.concurrent.duration._

/** Representation of a message being sent from one actor to another
  */
case class SendMessage(
    from: Actor,
    to: Actor,
    timestamp: kind.logic.Timestamp,
    duration: Duration,
    arrow: String, // the mermaid arrow. This is a bit hacky
    message: Any,
    comment: String = ""
) {
  // def endTimestamp: kind.logic.Timestamp = timestamp.addDuration(duration)
  def endTimestamp: kind.logic.Timestamp = (timestamp.asNanos + duration.toNanos).asTimestampNanos

  def isActiveAt(time: Timestamp) = {
    (timestamp.asNanos <= time.asNanos) && (time.asNanos <= endTimestamp.asNanos)
  }

  def messageFormatted = message match {
    case json: ujson.Value => json.render(2)
    case other             => other.toString()
  }

  private def truncate(owt: Any, len: Int = 85) =
    val opString = owt.toString
    if opString.length > len then opString.take(len - 3) + "..." else opString

  def asMermaidString(maxLenComment: Int = 20) = {
    s"${from} $arrow ${to} : ${truncate(messageFormatted, maxLenComment)} ${truncate(comment, 30)}"
  }
}

object SendMessage {

  /** Adjust the timestamps so that the timestamps and durations fit in the given duration
    *
    * @param messages
    *   the messages to scale
    * @param fitTo
    *   the desired time duration
    * @return
    *   the scaled messages
    */
  def scaleToFit(messages: Seq[SendMessage], fitTo: FiniteDuration): Seq[SendMessage] = {
    if messages.isEmpty then return messages

    val fromTimeNanos = messages.map(_.timestamp.asNanos).min
    val scaleFactor = {
      val toTimeNanos = messages.map(_.endTimestamp.asNanos).max

      fitTo.toNanos.toDouble / (toTimeNanos - fromTimeNanos)
    }

    messages.map { msg =>
      val scaledDelta = ((msg.timestamp.asNanos - fromTimeNanos) * scaleFactor).toLong
      msg.copy(
        timestamp = (fromTimeNanos + scaledDelta).asTimestampNanos,
        duration = (msg.duration * scaleFactor)
      )
    }
  }

  /** This recursive function walks through the calls, keeping track of which participants are
    * currently active.
    *
    * If a call starts and then completes before the next call (i.e. synchronous invocation), we
    * don't bother activating a participant, and we don't add to the sortedCompleted, we just add a
    * "source -->> target" line
    *
    * If the next call DOES come before the current call's end timestamp, then we add a "source
    * -->>+ target" line and add the call to the sortedCompleted (which stays sorted) We then add a
    * target -->>- source line for the return call which deactivates the target participant
    *
    * @param sortedCalls
    *   the full sorted call stack we're walking through
    * @param sortedCompleted
    *   our buffer of async calls which have completed after the next call
    * @param buffer
    *   the buffer of statements we're appending to
    * @return
    *   a sequence of mermaid statements
    */
  def fromCalls(
      sortedCalls: Seq[CompletedCall],
      sortedCompleted: Seq[CompletedCall] = Vector(),
      buffer: Seq[SendMessage] = Vector()
  ): Seq[SendMessage] = {

    def selfCall(call: CompletedCall): SendMessage = {
      SendMessage(
        call.source,
        call.source, // check: This was cal.target in Telemetry
        call.timestamp,
        call.duration.getOrElse(Duration.Inf),
        "->>",
        call.operation
      )
    }

    def startCall(call: CompletedCall, arrow: String): SendMessage = {
      // s"${call.source} $arrow ${call.target} : ${truncate(call.operation)}"
      SendMessage(
        call.source,
        call.target,
        call.timestamp,
        call.duration.getOrElse(Duration.Inf),
        arrow,
        call.operation
      )
    }

    def commentForResult(call: CompletedCall) = call.response match {
      case CallResponse.NotCompleted         => "never completed"
      case CallResponse.Error(_, error)      => s"Errored with '$error'"
      case CallResponse.Completed(_, result) => s"Returned '$result'"
    }

    def endCall(call: CompletedCall, arrow: String): Option[SendMessage] = call.response match {
      case CallResponse.NotCompleted => None
      case _                         =>
        // Some(s"${call.target} $arrow ${call.source} : ${commentForResult(call)}")
        Option(
          SendMessage(
            call.target,
            call.source,
            call.timestamp,
            call.duration.getOrElse(Duration.Inf),
            arrow,
            call.operation,
            commentForResult(call)
          )
        )
    }

    // NOTE - this is technically inefficient, but we're talking about lists of 2 or 3 items
    def appendInProgress(call: CompletedCall) =
      (call +: sortedCompleted).sortBy(_.endTimestamp.fold(-1L)(_.asNanos))

    (sortedCalls, sortedCompleted) match {
      case (Seq(), Seq()) => buffer
      case (Seq(), nextCompleted +: theRestCompleted) =>
        fromCalls(
          Seq(),
          theRestCompleted,
          buffer :++ endCall(nextCompleted, "-->>-")
        )
      case (Seq(nextCall), Seq()) =>
        buffer :+ startCall(nextCall, "->>")
      // the case when our next completion ends before or at the same time as the next call:
      case (nextCall +: theRestCalls, inProgress +: theRestInProgress)
          if inProgress.endTimestamp.exists(_.asNanos <= nextCall.timestamp.asNanos) =>
        fromCalls(
          nextCall +: theRestCalls,
          theRestInProgress,
          buffer :++ endCall(inProgress, "-->>-")
        )
      // the case when our next call doesn't complete until after the subsequent call:
      case (callA +: callB +: theRestCalls, _)
          if callA.endTimestamp.exists(_.asNanos > callB.timestamp.asNanos) =>
        fromCalls(
          callB +: theRestCalls,
          appendInProgress(callA),
          buffer :+ startCall(callA, "->>+")
        )
      // the typical synchronous call case
      case (callA +: theRestCalls, theRestInProgress) =>
        val calls = if callA.source == callA.target then {
          selfCall(callA) :: Nil
        } else {
          startCall(callA, "->>") +: endCall(callA, "-->>").toSeq
        }
        fromCalls(
          theRestCalls,
          theRestInProgress,
          buffer :++ calls
        )
    }
  }
}
