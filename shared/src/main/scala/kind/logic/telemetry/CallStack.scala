package kind.logic.telemetry

import zio.{Ref, Task, UIO, ZIO}

import scala.concurrent.duration.FiniteDuration

private[telemetry] case class CallStack(calls: Seq[Call] = Vector()) {
  def add(call: Call) = copy(calls :+ call)
}
