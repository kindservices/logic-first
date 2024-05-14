package kind.logic.telemetry

private[telemetry] case class CallStack(calls: Seq[Call] = Vector()) {
  def add(call: Call) = copy(calls :+ call)
}
