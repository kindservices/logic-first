package kind.logic.telemetry

import kind.logic._
import kind.logic.color._
import zio._

import java.util.concurrent.TimeUnit

/** Telemetry is a trait which allows us to track the calls made in our system, which we can later
  * use to show what happened
  *
  * @param callsStackRef
  */
trait Telemetry(val callsStackRef: Ref[CallStack]) {

  /** reset the callstack
    * @param f
    *   a transformation function
    * @return
    *   an action which updates the callstack
    */
  def reset(f: CallStack => CallStack = _ => CallStack()): UIO[Unit] = callsStackRef.update(f)

  def size(): UIO[Int] = callsStackRef.get.map(_.calls.size)

  def mermaid = Mermaid(calls)

  def pretty = calls.execOrThrow().mkString("\n")

  def calls: UIO[Seq[CompletedCall]] = {
    for
      callStack <- callsStackRef.get
      calls     <- ZIO.foreach(callStack.calls)(_.asCompletedCall)
    yield calls
  }

  def onCall[F[_], A](action: Action, input: Any): ZIO[Any, Nothing, Call] = {
    for
      call <- Call(action, input)
      _    <- callsStackRef.update(_.add(call))
    yield call
  }
}

object Telemetry {
  def apply(): Telemetry = make().execOrThrow()
  def make() = {
    for calls <- Ref.make(CallStack())
    yield new Telemetry(calls) {}
  }
}

def now = Clock.currentTime(TimeUnit.NANOSECONDS)
