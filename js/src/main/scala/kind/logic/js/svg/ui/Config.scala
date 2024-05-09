package kind.logic.js.svg.ui

import scala.concurrent.duration.*
import org.scalajs.dom
case class ActorConfig(
    // how much further down should the actor labels be from the actor icon?
    labelYOffset: Int = 40,
    // how big is the text size do we think (hack for laying out actors in a circle)
    estimatedTextHeight: Int = 20,
    // how much should we scale the emoji icons?
    iconScale: Int = 3,
    // how big of a gap should we have between categories
    categoryGap: Degrees = 5.degrees,
    // how thick is the category arch?
    categoryThickness: Int = 180
):
  def fullHeight = labelYOffset + estimatedTextHeight
end ActorConfig

case class Config(
    width: Int,
    height: Int,
    padding: Int,
    svgStyle: String = "background-color: white;",
    actorConfig: ActorConfig = ActorConfig(),
    playIncrement: Int = 20,
    animationRefreshRate: Duration = 40.millis
):
  def fullHeight = height + padding + actorConfig.fullHeight
  def center     = Point(width / 2, height / 2)
  def radius     = (width.min(height) - padding - actorConfig.fullHeight) / 2

object Config:
  def docHeight         = dom.window.innerHeight * 0.8
  def docWidth          = dom.window.innerWidth * 0.8
  def default(): Config = new Config(docWidth.toInt, docHeight.toInt, 300)
end Config
