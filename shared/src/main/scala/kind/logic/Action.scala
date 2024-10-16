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

  /** this is a builder method which assumes a source Container is in scope.
    *
    * That container 'calls' another container with a given action.
    *
    * @param target
    *   the target container
    * @param operationName
    *   the name of the operation (function) - otherwise it will be derived using the enclosing
    *   scope
    * @param source
    *   the source container of this action
    * @param enclosingScope
    *   used to default the operation name if not supplied
    * @return
    *   an Action
    */
  def calls(target: Container, operationName: String = null)(using
      source: Container,
      enclosingScope: sourcecode.Enclosing
  ): Action = {
    val op = Option(operationName).getOrElse(operation)
    Action(source, target, op)
  }

  /** 'operation' assumes it is being called within the business logic handler of a function.
    *
    * It's both a convenience method for getting the action name, as well as a means to avoid
    * duplication (e.g. having to write and maintain the method name next to the method)
    *
    * @param src
    *   the sourcecode of this call
    * @return
    *   the name of the operation from the enclosing function
    */
  def operation(using src: sourcecode.Enclosing): String = {
    // the enclosing is something like:
    // kind.examples.simple.App#nested.assets.Onboarding.apply $anon#create
    val parts = src.value.split("#").toSeq
    if parts.length < 2 then {
      val pathParts = src.value.split("\\.").toSeq
      if pathParts.length < 2 then {
        sys.error(s"We couldn't derive the operation from ${src.value} split on '#' or '.'. As a fix, just don't use 'Action.calls' for ${src.value}")
      } else pathParts.last.replaceAll("\\$1", "").trim
    } else
      parts.last.replaceAll("\\$1", "").trim
  }
}
