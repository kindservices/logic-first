package kind.logic.telemetry

import kind.logic.*
import zio.{UIO, *}

import java.util.concurrent.TimeUnit

/** Telemetry is a trait which allows us to track the calls made in our system, which we can later
  * use to show what happened
  *
  * @param callsStackRef
  */
trait Telemetry(val callsStackRef: Ref[CallStack], val counter: Ref[Long]) {

  private[telemetry] def nextId() = counter.updateAndGet(_ + 1)

  /** reset the callstack
    * @param f
    *   a transformation function
    * @return
    *   an action which updates the callstack
    */
  def reset(f: CallStack => CallStack = _ => CallStack()): UIO[Unit] = callsStackRef.update(f)

  def size(): UIO[Int] = callsStackRef.get.map(_.calls.size)

  def mermaid: UIO[Mermaid] = calls.map(c => Mermaid(c))

  def c4: UIO[C4] = calls.map(c => C4(c))

  def pretty: String = calls.execOrThrow().mkString("\n")

  def calls: UIO[Seq[CompletedCall]] = {
    for
      callStack <- callsStackRef.get
      calls     <- ZIO.foreach(callStack.calls)(_.asCompletedCall)
    yield calls.sortBy(_.callId)
  }

  /** onCall is used to append a new call to the stack
    * @param action
    *   the compile-time known data (operations and source and target components)
    * @param input
    *   the data used in this call
    * @return
    *   the call
    */
  def onCall(action: Action, input: Any): UIO[Call] = {
    for
      id   <- nextId()
      call <- Call(id, action, input)
      _    <- callsStackRef.update(_.add(call))
    yield call
  }
}

object Telemetry {
  def apply(): Telemetry = make().execOrThrow()
  def make() = {
    for
      calls   <- Ref.make(CallStack())
      counter <- Ref.make(0L)
    yield new Telemetry(calls, counter) {}
  }
}

def now = Clock.currentTime(TimeUnit.NANOSECONDS)
