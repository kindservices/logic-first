package kind.logic.telemetry

import kind.logic.*
import ujson.Value

import scala.concurrent.duration.Duration
import scala.concurrent.duration.*

/** Representation of a message being sent from one actor to another
  */
case class SendMessage(
    from: Container,
    to: Container,
    timestamp: kind.logic.Timestamp,
    duration: Duration,
    arrow: String, // the mermaid arrow. This is a bit hacky
    operation: String,
    input: Any,
    comment: String = ""
) {
  // def endTimestamp: kind.logic.Timestamp = timestamp.addDuration(duration)
  def endTimestamp: kind.logic.Timestamp = (timestamp.asNanos + duration.toNanos).asTimestampNanos

  def isActiveAt(time: Timestamp) = {
    (timestamp.asNanos <= time.asNanos) && (time.asNanos <= endTimestamp.asNanos)
  }

  private def chompQuotes(str: String) = str match {
    case other if other.startsWith("\"") && other.endsWith("\"") => other.init.tail
    case other                                                   => other
  }

  def messageFormatted = s"$operation($inputFormatted)"

  def inputFormatted = input match {
    case json: ujson.Value =>
      // we treat the 'action' key specially
      json.objOpt.flatMap(_.get("action")) match {
        case Some(action) =>
          json.obj.remove("action")
          s"${chompQuotes(action.render(0))} ${json.render(0)}"
        case None => json.render(0)
      }

    case other => Option(other).map(_.toString).getOrElse("")
  }

  private def truncate(owt: Any, len: Int = 85) =
    val opString = owt.toString.linesIterator.mkString("") // remove newlines
    if opString.length > len then opString.take(len - 3) + "..." else opString

  def asMermaidString(maxLenComment: Int = 20, maxComment: Int = 30) = {
    val msg =
      if comment.nonEmpty then truncate(comment, maxComment)
      else truncate(messageFormatted, maxLenComment)
    s"${from.qualified} $arrow ${to.qualified} : $msg "
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

  private def commentForResult(call: CompletedCall) = call.response match {
    case CallResponse.NotCompleted         => "never completed"
    case CallResponse.Error(_, error)      => s"Errored with '$error'"
    case CallResponse.Completed(_, result) => s"$result"
  }
  private enum Msg:
    case Start(id: Int, call: CompletedCall)
    case End(startId: Int, endTimestamp: Timestamp, call: CompletedCall)
    def timestamp: Timestamp = this match {
      case Start(_, call)          => call.timestamp
      case End(_, endTimestamp, _) => endTimestamp
    }

  def from(calls: Seq[CompletedCall]): Seq[SendMessage] = {
    val messages: IndexedSeq[Msg] = {
      val startMessages: Seq[Msg.Start] = calls.zipWithIndex.map { case (c, i) =>
        Msg.Start(i, c)
      }
      val endMessages = startMessages.flatMap { msg =>
        msg.call.endTimestamp.map { endTimestamp =>
          Msg.End(msg.id, endTimestamp, msg.call)
        }
      }
      (startMessages ++ endMessages).toIndexedSeq.sortBy(_.timestamp.asNanos)
    }

    messages.zipWithIndex.map {
      case (Msg.Start(msgId, call), i) =>
        val arrow =
          if call.source == call.target then "->>"
          else {
            // if the next message is anything other than the response to this message,
            // then we represent that as an async arrow: "->>+"
            val opt = messages.lift.apply(i + 1)
            val isSynchronous = opt.exists {
              case Msg.End(sourceId, _, _) => sourceId == msgId
              case _                       => false
            }

            if isSynchronous then "->>" else "->>+"
          }
        SendMessage(
          call.source,
          call.target,
          call.timestamp,
          call.duration.getOrElse(Duration.Inf),
          arrow,
          call.operation,
          call.input
        )
      case (Msg.End(startId, timestamp, call), i) =>
        // if the previous call was the start of this call, then its synchronous. Otherwise not
        val arrow = {
          val isSynchronous = messages.lift.apply(i - 1).exists {
            case Msg.Start(id, _) => id == startId
            case _                => false
          }
          if isSynchronous then "-->>" else "-->>-"
        }

        SendMessage(
          call.target,
          call.source,
          call.timestamp,
          call.duration.getOrElse(Duration.Inf),
          arrow,
          call.operation,
          call.input,
          commentForResult(call)
        )
    }
  }

}
