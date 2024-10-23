package kind.logic.dsl
import kind.logic.*
import kind.logic.telemetry.Telemetry
import zio.*

case class Call(target: Container, operationName: String = null)(using
    source: Container,
    telemetry: Telemetry,
    scope: sourcecode.Enclosing
) {
  val op     = Option(operationName).getOrElse(Action.operation)
  val action = Action(source, target, op)

  /** continue the 'dsl' by adding 'withArgs' to specify the function args
    *
    * @param args
    *   the arguments
    * @param f
    *   the function
    * @tparam T
    *   the return type
    * @return
    *   the function as a traced task
    */
  def withArgs[T](args: Any)(f: => T)(implicit ev: T =!= Task[?] = null): Task[T] = {
    f.asTask.traceWith(action, args) // Apply asTask if T is not a Task
  }

  /** continue the 'dsl' by adding 'withArgs' to specify the function args
    *
    * @param args
    *   the arguments
    * @param f
    *   the function
    * @tparam T
    *   the return type
    * @return
    *   the function as a traced task
    */
  def withArgs[T](args: Any)(f: => Task[T]): Task[T] = f.traceWith(action, args)

  /** A special case where an 'f' function returns a task
    *
    * @param f
    *   the action
    * @return
    *   the function as a traced task
    */
  def apply[T](f: => Task[T]): Task[T] = f.traceWith(action)

  /** Turns the 'f' function into a traced task
    *
    * @param f
    *   the action
    * @return
    *   the function as a traced task
    */
  def apply[T](f: => T)(implicit ev: T =!= Task[?] = null): Task[T] = {
    f.asTask.traceWith(action) // Apply asTask if T is not a Task
  }
}
