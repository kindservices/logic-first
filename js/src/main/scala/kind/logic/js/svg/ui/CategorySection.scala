package kind.logic.js.svg.ui

import kind.logic.Actor
import kind.logic.color.Colors
import kind.logic.js.svg._
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.Node
import scalatags.JsDom
import scalatags.JsDom.implicits._
import scalatags.JsDom.svgAttrs._
import scalatags.JsDom.svgTags._
import scalatags.JsDom.{svgTags => stags}

/** This is probably the hardest part of this whole thing ... the trigonometry to lay out the actors
  * in a circle!
  *
  * We want a background arc behind components in the same category. We then want to lay out the
  * actors in that category, all equally spaced.
  *
  * @param category
  *   the name of the category
  * @param actorsInThisCategory
  *   the actors (people, systems, etc) in this category, along with their center points on the
  *   circle
  * @param color
  *   the color of the background arc
  * @param arcStart
  *   the background arc start angle
  * @param arcEnd
  *   the background arc end angle
  * @param config
  */
case class CategorySection(
    category: String,
    actorsInThisCategory: Seq[(Actor, Point)],
    color: String,
    arcStart: Degrees,
    arcEnd: Degrees,
    config: Config
) {
  require(arcEnd.toDouble > arcStart.toDouble)

  def actorComponents: Seq[Element] = {

    actorsInThisCategory.size
    (arcEnd - arcStart) / (actorsInThisCategory.size + 1)

    actorsInThisCategory.map { case (actor, center) =>
      val yOffset = center.y + config.actorConfig.labelYOffset

      import center.*

      g(
        stags.text(
          fill             := "white",
          stroke           := "black",
          textAnchor       := "middle",
          dominantBaseline := "middle",
          transform        := s"translate($x,$y),scale(${config.actorConfig.iconScale})"
        )(actor.`type`.icon),
        stags.text(
          textAnchor       := "middle",
          dominantBaseline := "middle",
          transform        := s"translate($x,$yOffset)"
        )(s"${actor.label}")
      ).render
    }
  }

  def backgroundArcComponents: Seq[Node] = {

    val thickness = config.actorConfig.categoryThickness

    val backgroundArc = Arc(config.center, config.radius, arcStart, arcEnd)
      .asSvg(
        thickness,
        color,
        s"arc-${category.filter(_.isLetterOrDigit)}",
        ""
      )

    // draw a second arc for the label on the outside (e.g. radius + half the thickness + 20 pixels)
    val labelRadius = config.radius + thickness / 2 + config.actorConfig.estimatedTextHeight
    val labelArc = Arc(config.center, labelRadius, arcStart, arcEnd)
      .asSvg(
        1,
        "white",
        s"arclabel-${category.filter(_.isLetterOrDigit)}",
        category
      )
    Seq(backgroundArc, labelArc)
  }
}

object CategorySection {

  def forActors(actors: Set[Actor], config: Config): Seq[CategorySection] = {
    val actorsByCateogory: Map[String, Set[Actor]] = actors.groupBy(_.category)

    val colors = Colors(actorsByCateogory.size)

    val gapOffset = config.actorConfig.categoryGap / 2

    val actorPoints: Seq[Point] = {

      // we space each actor evenly around the circle
      val actorAngleStepSize = 360.degrees / actors.size
      // and shift them all by half to center them
      val initialActorAngleOffset = actorAngleStepSize / 2

      actors.toSeq.zipWithIndex.map { case (_, i) =>
        val angle = initialActorAngleOffset + (actorAngleStepSize * i)

        val x = config.center.x + config.radius * Math.cos(angle.asRadians)
        val y = config.center.y + config.radius * Math.sin(angle.asRadians)

        Point(x.toInt, y.toInt)
      }
    }

    // we're trying to build up the layout for each set of 'actors' in the system, with a background arc behind that category
    //
    // to do that, we first create the angle ranges, which is the percentage size of the whole circle per category
    // (that is, if one category has 3 actors and another has 6, the first will have 1/3 of the circle and the second 2/3)
    //
    // then we create the actual sections, which are the arcs that will be drawn behind the actors
    //
    // this really ugly fold is just to keep track of the start and end angles for each category as we build up the sections
    //
    // we start with 0 degrees and an empty list of sections, and then for each category we calculate the start and end angles
    val (_, _, categories) =
      actorsByCateogory
        .zip(colors)
        .foldLeft((0.degrees, 0, Seq.empty[CategorySection])) {

          // start is the start angle for the current category, and sections is the list of sections we've built up so far
          //
          // category is just the the actorsByCateogory map entry, which is the name of the category, and actorsForCategory is the actors in that category
          //
          // we also have zipped together the categories with a color for that category
          case ((start, count, sections), ((category, actorsForCategory), color)) =>
            //
            // what percentage of the whole circle do these actors account for?
            val proportionalArcSize: Degrees = {
              val ratioOfThisCategory = actorsForCategory.size / actors.size.toDouble
              360.degrees * ratioOfThisCategory
            }

            // the end angle is the start angle plus the proportional arc size, minus the gap between categories
            val end            = start + proportionalArcSize - gapOffset
            val nextStartAngle = start + proportionalArcSize

            // we're going to build up the list of actors in this category, with their positions
            val actorAngles: Seq[(Actor, Point)] =
              actorsForCategory.toSeq.sortBy(_.label).zipWithIndex.map { (actor, i) =>
                val actorIndex  = count + i
                val actorCenter = actorPoints(actorIndex)
                (actor, actorCenter)
              }

            val newSection = CategorySection(
              category,
              actorAngles,
              color,
              start + gapOffset,
              end,
              config
            )

            (nextStartAngle, count + actorsForCategory.size, sections :+ newSection)
        }

    categories
  }

}
