package kind.logic

import zio.*

import telemetry.*
import scala.annotation.targetName
import scala.reflect.ClassTag

/** This trait is a kind of convenience wrapper around our 'Program' ADT, providing the common (but
  * perhaps unfamiliar or confusing) 'foldMap' machinery we need to execute our program.
  */
abstract class RunnableProgram[F[_]](val logic: [A] => F[A] => Result[A])(using
    telemetry: Telemetry
) {

  /** Run will execute this program using the 'onInput' to determine how to implement each operation
    *
    * @param fa
    *   the program to run
    * @tparam A
    *   the result type
    * @return
    *   a runnable Task
    */
  def run[A](fa: Program[F, A], trace: Boolean = true): Task[A] = {
    @targetName("programAsTask")
    given ~>[F, Task] with {
      override def apply[A](fa: F[A]): Task[A] =
        if trace then traceOnInput(fa) else onInput(fa).task
    }
    fa.foldMap[Task]
  }

  /** we need to get at the subclass 'Actor', not this trait's name (otherwise everything will just
    * look like a RunnableProgram)
    * @return
    *   the 'coords' (category and service name) of this application. Typically just return
    *   'Actor(this)' in sublasses
    */
  protected def appCoords: Actor

  /** How do we want to handle the execution of our program?
    *
    * @param operation
    *   the input operation
    * @return
    *   either a simple task if it's done in-process, or an 'Invoke' if it relies on calling another
    *   service
    */
  protected def onInput[A](operation: F[A]): Result[A] = logic(operation)

  /** Our 'traceOnInput' will add a call to our telemetry, giving us insight on the operations of
    * our program
    * @param operation
    *   the operation to run
    * @tparam A
    *   the return type
    * @return
    *   the Task result of this operation, with the side-effect of tracking the invocation
    */
  private def traceOnInput[A](operation: F[A]): Task[A] = {
    onInput(operation) match {
      case Result.RunTask(task) => task // don't add a telemetry call
      case Result.TraceTask(targetCoords, job, inputOpt) =>
        traceTask(job, appCoords, targetCoords, inputOpt.getOrElse(operation))
    }
  }
}
