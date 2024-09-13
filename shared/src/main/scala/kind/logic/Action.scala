package kind.logic
import upickle.default.*

/** Actions represent the components of a call known at compile time:
  *
  * @param source
  *   the source of the action
  * @param target
  *   the recipient of the action
  * @param operation
  *   the name of the action
  */
case class Action(source: Container, target: Container, operation: String) derives ReadWriter

object Action {

  def operation(using src: sourcecode.Enclosing): String = {
    // the enclosing is something like:
    // kind.examples.simple.App#nested.assets.Onboarding.apply $anon#create
    val parts = src.value.split("#").toSeq
    if parts.length < 2 then sys.error(s"BUG: don't use 'Action.calls' for ${src.value}")
    parts.last
  }

  def calls(target: Container, operationName: String = null)(using
      source: Container,
      src: sourcecode.Enclosing
  ): Action = {
    val op = Option(operationName).getOrElse(operation)
    Action(source, target, op)
  }
}
