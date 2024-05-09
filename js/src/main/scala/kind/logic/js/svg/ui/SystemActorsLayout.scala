package kind.logic.js.svg.ui

import kind.logic.js.svg.*
import kind.logic.Actor
import scalatags.JsDom
import scalatags.JsDom.TypedTag
import org.scalajs.dom.Node

/** This class encapsulates the logic for laying out the system actors (the databases, people,
  * services, etc) and their categories
  *
  * @param actors
  * @param config
  */
class SystemActorsLayout(actors: Set[Actor], config: Config):

  val sections: Seq[CategorySection] = CategorySection.forActors(actors, config)

  // where are all our actors? we need this so we can lay out our messages
  val positionByActor: Map[Actor, Point] = sections.foldLeft(Map[Actor, Point]()) {
    (acc, section) =>
      acc ++ section.actorsInThisCategory
  }

  def components: Seq[Node] =
    sections.flatMap(_.backgroundArcComponents) ++ sections.flatMap(_.actorComponents)

end SystemActorsLayout

object SystemActorsLayout {

  /** given a center point, lay out N points around it in a circle
    */
  private def pointsAroundCenterpoint(center: Point, radius: Int, n: Int): List[(Int, Int)] = {
    import center.*
    val angle = 2 * Math.PI / n
    (0 until n)
      .map(i => {
        val newX = x + radius * Math.cos(i * angle)
        val newY = y + radius * Math.sin(i * angle)
        (newX.toInt, newY.toInt)
      })
      .toList
  }
}
