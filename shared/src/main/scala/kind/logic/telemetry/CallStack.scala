package kind.logic.telemetry

case class CallStack(calls: Seq[Call] = Vector()) {
  def add(call: Call) = copy(calls :+ call)
}
