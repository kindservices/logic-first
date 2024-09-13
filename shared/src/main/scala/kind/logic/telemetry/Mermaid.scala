package kind.logic.telemetry

import kind.logic._
import kind.logic.color._
import zio._
import kind.logic.Actor
import kind.logic.color.Colors
import zio.UIO

object Mermaid {

  val DefaultMermaidStyle = """%%{init: {"theme": "dark",
"themeVariables": {"primaryTextColor": "grey", "secondaryTextColor": "black", "fontFamily": "Arial", "fontSize": 14, "primaryColor": "#3498db"}}}%%"""

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
  private def asMermaidStatements(
      sortedCalls: Seq[CompletedCall],
      maxLenComment: Int,
      maxComment: Int
  ): Seq[String] = {
    // our completed calls are the inverse of the original calls -- the responses which come back from the invocations
    val sortedCompleted         = sortedCalls.flatMap(_.inverse).sortBy(_.timestamp.asNanos)
    val calls: Seq[SendMessage] = SendMessage.fromCalls(sortedCalls, sortedCompleted)

    calls.map(_.asMermaidString(maxLenComment, maxComment))
  }
}
case class Mermaid(calls: UIO[Seq[CompletedCall]]) {

  import Mermaid.*

  def asMermaid(
      mermaidStyle: String = DefaultMermaidStyle,
      maxLenComment: Int = 60,
      maxComment: Int = 30
  ): UIO[String] =
    diagram(mermaidStyle, maxLenComment, maxComment).map { mermaidMarkdown =>
      mermaidMarkdown
        .replace("```mermaid", "")
        .replace("```", "")
        .trim
    }

  /** @return
    *   a operation which will access the trace calls and render them as a mermaid block
    */
  def diagram(
      mermaidStyle: String = DefaultMermaidStyle,
      maxLenComment: Int = 60,
      maxComment: Int = 30
  ): UIO[String] =
    sequenceDiagram(maxLenComment, maxComment).map(sd => s"\n```mermaid\n$mermaidStyle\n${sd}```\n")

  /** This is just the sequence block part of the mermaid diagram. See 'asMermaidDiagram' for the
    * full markdown version
    *
    * @return
    *   the calls as a mermaid sequence diagram
    */
  def sequenceDiagram(maxLenComment: Int, maxComment: Int): UIO[String] = calls.map { all =>
    val statements =
      asMermaidStatements(all.sortBy(_.timestamp.asNanos), maxLenComment, maxComment)

    // Here we group the participants by category to produce (1) a sorted list of the categories and (2) the actors by category
    (participants(all) ++ statements).mkString("sequenceDiagram\n\t", "\n\t", "\n")
  }

  def participants(all: Seq[CompletedCall]): Seq[String] = {
    val (orderedCategories, actorsByCategory) = all
      .sortBy(_.timestamp.asNanos)
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
          .map(a => s"participant ${a.qualified}")
        List(s"box $color $category") ++ participants ++ List("end")
      }
  }

}
