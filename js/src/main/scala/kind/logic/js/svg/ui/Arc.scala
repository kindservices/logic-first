package kind.logic.js.svg.ui

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import org.scalajs.dom
import scalatags.JsDom

// import scalatags.JsDom.all.*
import scalatags.JsDom.{svgTags => stags}
import scalatags.JsDom.svgTags.*
import scalatags.JsDom.svgAttrs.*
import scalatags.JsDom.implicits.{given, *}

import scala.scalajs.js.annotation.*
import scala.concurrent.duration.{given, *}
import org.scalajs.dom.Element

/** 🤮🤮🤮 Chuffing SVG's layout of an arc sucks.... you have to give it points on the arc, the
  * radius, and ...🤮🤮🤮
  *
  * It's just not what we want ... we want a center points, a radius, and a start and end angle.
  * That's it.
  *
  * This data structure translates that into the SVG path that we need.
  */
case class Arc(center: Point, radius: Int, startAngle: Degrees, endAngle: Degrees) {
  def asSvg(arcWidth: Int, color: String, pathId: String, label: String): Element = {
    val startX = center.x + (radius * Math.cos(endAngle.asRadians))
    val startY = center.y + (radius * Math.sin(endAngle.asRadians))

    val endX = center.x + (radius * Math.cos(startAngle.asRadians))
    val endY = center.y + (radius * Math.sin(startAngle.asRadians))

    val largeArcFlag =
      if ((endAngle - startAngle).toInt % 360 > 180) then 1 else 0 // Large arc flag
    val sweepFlag = 0

    g(
      path(
        id     := pathId,
        d      := s"M $startX $startY A $radius $radius 0 $largeArcFlag $sweepFlag $endX $endY",
        stroke := color,
        strokeWidth := arcWidth,
        fill        := "none"
      ),
      text(fontSize := 20)(
        textPath(xLinkHref := s"#${pathId}", stags.attr("startOffset") := "35%")(label)
      )
    ).render
  }
}
