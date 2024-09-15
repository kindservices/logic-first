package kind.logic
import upickle.default.*

case class Action(source: Container, target: Container, operation: String) derives ReadWriter {
  def flip(newOperation: String) = copy(source = target, target = source, operation = newOperation)
}

object Action {

  def operation(using obj: sourcecode.Enclosing): String = {
    obj.value
  }

  def calls(target: Container, operationName: String = null)(using
      source: Container,
      obj: sourcecode.Enclosing
  ): Action = {
    println(s"op is ${operation}")
    val op = Option(operationName).getOrElse(operation)
    Action(source, target, op)
  }
}
