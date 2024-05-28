package kind.logic

import kind.logic.telemetry.Telemetry
import zio.{Task, *}

enum Result[A]:
  case RunTask(job: Task[A])                                             extends Result[A]
  case TraceTask(coords: Actor, job: Task[A], input: Option[Any] = None) extends Result[A]

//  def task: Task[A] = this match {
//    case RunTask(task)         => task
//    case TraceTask(_, task, _) => task
//  }

  def asTask(calledFrom: Actor, input: Any = null)(using telemetry: Telemetry) = this match {
    case RunTask(task) => task
    case TraceTask(targetCoords, job, inputOpt) =>
      val param = inputOpt.orElse(Option(input)).getOrElse("input not specified")
      traceTask(job, calledFrom, targetCoords, param)
  }
