package kind.logic
import upickle.default.*

case class Action(source: Container, target: Container, operation: String) derives ReadWriter {
  def flip(newOperation: String) = copy(source = target, target = source, operation = newOperation)
}

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
