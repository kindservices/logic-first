package kind.logic.telemetry

import kind.logic.*
import kind.logic.color.*
import zio.*
import java.util.concurrent.TimeUnit
import scala.reflect.ClassTag
import scala.concurrent.duration.{given, *}

/** Telemetry is a trait which allows us to track the calls made in our system, which we can later
  * use to show what happened
  *
  * @param callsStackRef
  */
trait Telemetry(val callsStackRef: Ref[CallStack]) {

  /** @return
    *   a operation which will access the trace calls and render them as a mermaid block
    */
  def asMermaidDiagram(
      mermaidStyle: String = """%%{init: {"theme": "dark", 
"themeVariables": {"primaryTextColor": "grey", "secondaryTextColor": "black", "fontFamily": "Arial", "fontSize": 14, "primaryColor": "#3498db"}}}%%"""
  ): UIO[String] =
    asMermaidSequenceDiagram.map(sd => s"\n```mermaid\n$mermaidStyle\n${sd}```\n")

  /** This is just the sequence block part of the mermaid diagram. See 'asMermaidDiagram' for the
    * full markdown version
    *
    * @return
    *   the calls as a mermaid sequence diagram
    */
  def asMermaidSequenceDiagram: UIO[String] = calls.map { all =>
    val statements = Telemetry.asMermaidStatements(all.sortBy(_.timestamp))

    // we want to group the participants together by category
    // this
    val participants = {
      val (orderedCategories, actorsByCategory) = all
        .sortBy(_.timestamp)
        .foldLeft((Seq[String](), Map[String, Seq[Actor]]())) {
          case ((categories, coordsByCategory), call) =>
            val newMap = coordsByCategory
              .updatedWith(call.source.category) {
                case None                                => Some(Seq(call.source))
                case Some(v) if !v.contains(call.source) => Some(v :+ call.source)
                case values                              => values
              }
              .updatedWith(call.target.category) {
                case None                                => Some(Seq(call.target))
                case Some(v) if !v.contains(call.target) => Some(v :+ call.target)
                case values                              => values
              }
            val newCategories = {
              val srcCat =
                if categories.contains(call.source.category) then categories
                else categories :+ call.source.category

              if srcCat.contains(call.target.category) then srcCat
              else srcCat :+ call.target.category
            }
            (newCategories, newMap)
        }

      orderedCategories
        .zip(Colors.namedColors.take(orderedCategories.size))
        .flatMap { (category, color) =>

          val participants = actorsByCategory
            .getOrElse(category, Nil)
            .map(a => s"participant $a")
          List(s"box $color $category") ++ participants ++ List("end")
        }
    }

    (participants ++ statements).mkString("sequenceDiagram\n\t", "\n\t", "\n")
  }

  def calls: UIO[Seq[CompletedCall]] = {
    for
      callStack <- callsStackRef.get
      calls     <- ZIO.foreach(callStack.calls)(_.asCompletedCall)
    yield calls
  }

  def onCall[F[_], A](source: Actor, target: Actor, input: Any): ZIO[Any, Nothing, Call] = {
    for
      call <- Call(source, target, input)
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

  /** This recursive function walks through the calls, keeping track of which participants are
    * currently active.
    *
    * If a call starts and then completes before the next call (i.e. synchronous invocation), we
    * don't bother activating a participant, and we don't add to the sortedCompleted, we just add a
    * "source -->> target" line
    *
    * If the next call DOES come before the current call's end timestamp, then we add a "source
    * -->>+ target" line and add the call to the sortedCompleted (which stays sorted) We then add a
    * target -->>- source line for the return call which deactivates the target participant
    *
    * @param sortedCalls
    *   the full sorted call stack we're walking through
    * @param sortedCompleted
    *   our buffer of async calls which have completed after the next call
    * @param mermaidBuffer
    *   the buffer of statements we're appending to
    * @return
    *   a sequence of mermaid statements
    */
  private def asMermaidStatements(sortedCalls: Seq[CompletedCall]): Seq[String] = {
    SendMessage.fromCalls(sortedCalls).map(_.asMermaidString())
  }
}

def now = Clock.currentTime(TimeUnit.NANOSECONDS)