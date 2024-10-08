package kind
import kind.logic.json._
import kind.logic.telemetry._
import ujson.Value.Value
import upickle.default._
import zio._

import scala.util.Try
package object logic {

  type Json             = Value
  opaque type Timestamp = Long

  /** Trace this call to the given 'target' service / database / whatever
    *
    * @param source
    *   where is this call coming from?
    * @param target
    *   what is the target of this call?
    * @param input
    *   the input used in this request
    * @return
    *   a new task which updates the telemetry with the call data when run
    */
  def traceTask[A](job: Task[A], action: Action, input: Any)(using
      telemetry: Telemetry
  ): Task[A] = {
    for
      call   <- telemetry.onCall(action, input)
      result <- call.completeWith(job, telemetry)
    yield result
  }

  extension (data: Json) {
    def as[A: ReadWriter]: Try[A] = Try(read[A](data))
  }

  extension (tsNanos: Long) {
    def asTimestampNanos: Timestamp                = tsNanos
    def +(duration: Duration): Timestamp           = tsNanos + duration.toNanos
    def addDuration(duration: Duration): Timestamp = tsNanos + duration.toNanos
    def *(scale: Double): Timestamp                = (tsNanos * scale).toLong
  }

  extension (ts: Timestamp) {
    def asNanos: Long  = ts
    def asMillis: Long = ts / 1000000
  }

  /** A common convenience method for ZIO stuff... might as well stick it here
    */
  extension [A](job: Task[A]) {

    def asTry(): Try[A] = Try(execOrThrow())

    def execOrThrow(): A = Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(job).getOrThrowFiberFailure()
    }

    /** This is handy when you just want to augment a Task which can update the Telemetry.
      *
      * It's a convenience method for just called 'traceTask', and useful when operating outside a
      * RunnableProgram
      *
      * @param calledFrom
      *   the source of the call
      * @param target
      *   the target of the call
      * @param input
      *   the input which triggered the task
      * @param telemetry
      *   the telemetry to update
      * @return
      *   a new Task which will update the Telemetry when run
      */
    def traceWith(action: Action, input: Any = null)(using
        telemetry: Telemetry
    ): Task[A] = traceTask(job, action, Option(input).getOrElse(()))

    /** @return
      *   this task a 'Result' (something convenient for RunnablePrograms to run)
      */
    def taskAsResult: Result[A] = Result.RunTask(job)

    /** @return
      *   this task a 'Result' (something convenient for RunnablePrograms to run)
      */
    def taskAsResultTraced(targetSystem: Container, action: String, input: Any = null): Result[A] =
      Result.TraceTask(targetSystem, job, action, Option(input))
  }

  extension [A: ReadWriter](value: A) {
    def asUJson = writeJs(value)

    /** This is useful for combining different objects to compose in a json result.
      *
      * For example:
      * {{{
      *   val someData = serviceCall()
      *   someData.withKey("data").merge
      * }}}
      * @param key
      * @return
      */
    def withKey(key: String): Map[String, A] = Map(key -> value)

    def withInput[B: ReadWriter](input: B) = input.withKey("input").merge(value)

    def asAction: Map[String, A] = withKey("action")

    /** Combines this object with another object as a json value
      * @param other
      *   the other object
      * @tparam B
      *   the other type
      * @return
      *   the merged json value
      */
    def merge[B: ReadWriter](other: B): Value = asUJson.mergeWith(other.asUJson)
  }

  // TODO: I tried using something like this to guard against people
  // accidentally pimping tasks types, but it didn't work:
  //
  // trait IsZIO[A]
  // given IsZIO: IsZIO[ZIO[?, ?, ?]] = new IsZIO[ZIO[?, ?, ?]] {}
  // (using NotGiven[IsZIO[A]])

  extension [A](op: => A) {
    def asTask: Task[A] =
      ZIO.attempt {
        op match {
          case task: ZIO[?, ?, ?] =>
            sys.error(
              s"BUG: You're pimping a ZIO type rather than an action. use .taskAsResult or .taskAsResultTraced"
            )
          case result => result
        }
      }

    /** @return
      *   this operation as a task inside a 'Result' type. The call will NOT be traced
      */
    def asResult: Result[A] = Result.RunTask(asTask)

    /** @param targetSystem
      *   who are we calling? Another system? Database? Queue? Filesystem? ...
      * @param input
      *   the input involved in this call
      * @return
      *   this operation as a task inside a traced 'Result' type. The call to the target will be
      *   traced when run inside a RunnableProgram
      */
    def asResultTraced(targetSystem: Container, action: String, input: Any = null): Result[A] =
      Result.TraceTask(targetSystem, asTask, action, Option(input))

  }
}
