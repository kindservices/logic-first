package kind.logic.telemetry

import kind.logic.*
import kind.logic.color.Colors

object Mermaid {

  val DefaultMermaidStyle = """%%{init: {"theme": "dark",
"themeVariables": {"primaryTextColor": "grey", "secondaryTextColor": "black", "fontFamily": "Arial", "fontSize": 14, "primaryColor": "#3498db"}}}%%"""

}

case class Mermaid(calls: Seq[CompletedCall]) {

  import Mermaid.*

  def asMermaid(
      mermaidStyle: String = DefaultMermaidStyle,
      maxLenComment: Int = 60,
      maxComment: Int = 30
  ): String = diagram(mermaidStyle, maxLenComment, maxComment)
    .replace("```mermaid", "")
    .replace("```", "")
    .trim

  /** @return
    *   a operation which will access the trace calls and render them as a mermaid block
    */
  def diagram(
      mermaidStyle: String = DefaultMermaidStyle,
      maxLenComment: Int = 60,
      maxComment: Int = 30
  ): String = s"\n```mermaid\n$mermaidStyle\n${sequenceDiagram(maxLenComment, maxComment)}```\n"

  /** @return
    *   the calls as a list of messages (calls and responses)
    */
  def callStack: Seq[SendMessage] = SendMessage.from(calls)

  /** This is just the sequence block part of the mermaid diagram. See 'asMermaidDiagram' for the
    * full markdown version
    *
    * @return
    *   the calls as a mermaid sequence diagram
    */
  def sequenceDiagram(maxLenComment: Int, maxComment: Int): String = {
    val stack                   = callStack
    val statements: Seq[String] = stack.map(_.asMermaidString(maxLenComment, maxComment))

    // Here we group the participants by category to produce (1) a sorted list of the categories and (2) the actors by category
    (participants(calls) ++ statements).mkString("sequenceDiagram\n\t", "\n\t", "\n")
  }

  private def participants(all: Seq[CompletedCall]): Seq[String] = {
    val (orderedCategories, actorsByCategory) = all
      .sortBy(_.timestamp.asNanos)
      .foldLeft((Seq[String](), Map[String, Seq[Container]]())) {
        case ((categories, coordsByCategory), call) =>
          val newMap = coordsByCategory
            .updatedWith(call.source.softwareSystem) {
              case None                                => Some(Seq(call.source))
              case Some(v) if !v.contains(call.source) => Some(v :+ call.source)
              case values                              => values
            }
            .updatedWith(call.target.softwareSystem) {
              case None                                => Some(Seq(call.target))
              case Some(v) if !v.contains(call.target) => Some(v :+ call.target)
              case values                              => values
            }
          val newCategories = {
            val srcCat =
              if categories.contains(call.source.softwareSystem) then categories
              else categories :+ call.source.softwareSystem

            if srcCat.contains(call.target.softwareSystem) then srcCat
            else srcCat :+ call.target.softwareSystem
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
