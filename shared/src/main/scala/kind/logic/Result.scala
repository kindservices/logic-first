package kind.logic

import kind.logic.telemetry.Telemetry
import zio.{Task, *}

/** A result is used as the return type (the result type) from a service function.
  *
  * For example:
  *
  * {{{
  *   def soSearch(filter : String) : Result[List[Name]] = ???
  * }}}
  *
  * The result can just be a zio Task, or optionally an instrumented TraceTask which captures
  * additional data for recording the system telemetry
  * @tparam A
  */
enum Result[A]:
  case RunTask(job: Task[A])                                             extends Result[A]
  case TraceTask(coords: Actor, job: Task[A], input: Option[Any] = None) extends Result[A]

  /** @return
    *   the task for this result
    */
  def task: Task[A] = this match {
    case RunTask(task)         => task
    case TraceTask(_, task, _) => task
  }

  /** @param calledFrom
    *   the calling system - used when this is a traced task
    * @param input
    *   the input (for this call), used when this is a traced task
    * @param telemetry
    *   the telemetry used when this is a traced task
    * @return
    *   the Result as a Task
    */
  def asTask(calledFrom: Actor, input: Any = null)(using telemetry: Telemetry): Task[A] =
    this match {
      case RunTask(task) => task
      case TraceTask(targetCoords, job, inputOpt) =>
        val param = inputOpt.orElse(Option(input)).getOrElse("input not specified")
        traceTask(job, calledFrom, targetCoords, param)
    }
