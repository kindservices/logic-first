package kind
import kind.logic.telemetry._
import zio._

package object logic {

  opaque type Timestamp = Long

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

    def execOrThrow(): A = Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(job).getOrThrowFiberFailure()
    }
    def taskAsResult: Result[A] = Result.RunTask(job)
    def taskAsResultTraced(targetSystem: Actor, input: Any = null): Result[A] =
      Result.TraceTask(targetSystem, job, Option(input))
  }

  /** Trace this call to the given 'target' service / database / whatever
    *
    * @param target
    *   the business name (Actor) of the target we're calling
    * @param input
    *   the input used in this request
    * @return
    *   a 'pimped' task which will update the telemetry when run
    */
  private def traceTask[A](job: Task[A], source: Actor, target: Actor, input: Any)(using
      telemetry: Telemetry
  ): Task[A] = {
    for
      call   <- telemetry.onCall(source, target, input)
      result <- call.completeWith(job)
    yield result
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

    /** This method is intended to be used when tasks are run within a single operation, for example
      * when we need to run things in parallel for a given input, so want to trace those resulting
      * actions.
      *
      * Everything else will be traced via the 'RunnableProgram' when we encounter an operation
      *
      * @param source
      *   the source system for this call (needed as we can run these tasks outside of
      *   RunnablePrograms)
      * @param target
      *   the target system - who we're calling
      * @param input
      *   the input used to make this call
      * @param telemetry
      *   the telemetry used to track calls
      * @return
      *   a task which will update the telemetry when run
      */
    def asTaskTraced(source: Actor, target: Actor, input: Any)(using
        telemetry: Telemetry
    ): Task[A] = traceTask(
      asTask,
      source,
      target,
      input
    )

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
    def asResultTraced(targetSystem: Actor, input: Any = null): Result[A] =
      Result.TraceTask(targetSystem, asTask, Option(input))

  }

}
