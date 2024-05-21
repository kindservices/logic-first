package kind.logic

import zio._

enum Result[A]:
  case RunTask(job: Task[A])                                             extends Result[A]
  case TraceTask(coords: Actor, job: Task[A], input: Option[Any] = None) extends Result[A]

  def task: Task[A] = this match {
    case RunTask(task)         => task
    case TraceTask(_, task, _) => task
  }
