package kind.logic.js.svg.ui

import kind.logic.js.svg.*
import org.scalajs.dom
import scalatags.JsDom.all.*
import scalatags.JsDom.implicits.given

import scala.concurrent.duration.{*, given}
import org.scalajs.dom.HTMLDivElement
import scalatags.JsDom.svgAttrs.{height, style, width, xmlns}
import scalatags.JsDom.svgTags.svg
import scalatags.Text.TypedTag

/** The container for the big circle thing showing the system actors and their messages
  *
  * @param systemActorLayout
  * @param messagesLayout
  * @param config
  *   the basic layout con
  */
class InteractiveMessageSvgComponent(
    systemActorLayout: SystemActorsLayout,
    messagesLayout: MessageLayout,
    config: Config
) {

  // is the animation playing?
  private var playing = true

  // is time slider mouse down?
  private var timeSliderMouseDown = false

  // how fast should we play?
  import config.{playIncrement, animationRefreshRate}

  private val containerDiv = div(width := config.width).render

  containerDiv.innerHTML = ""

  containerDiv.append(
    svg(
      width  := config.width,
      height := config.height,
      xmlns  := "http://www.w3.org/2000/svg",
      style  := config.svgStyle
    )(systemActorLayout.components ++ messagesLayout.elements).render
  )

  private val timeSlider = {
    val stepSize =
      (messagesLayout.maxTime - messagesLayout.minTime) / 100 // split the total time into 100 steps
    input(
      id     := "time-slider",
      `type` := "range",
      min    := messagesLayout.minTime,
      max    := messagesLayout.maxTime,
      value  := messagesLayout.minTime,
      step   := stepSize,
      style  := s"width: ${config.width * 0.6}px; margin-left: ${config.width * 0.2}px"
    ).render
  }
  private val timeSliderLabel = label(`for` := "time-slider")("Time:").render

  private def refresh() = {
    playPauseButton.value = playPauseLabel
    val time     = timeSlider.value.toInt
    val messages = messagesLayout.layout(time)
  }

  private def playPauseLabel = if playing then "Pause ⏸️" else "Play ▶️"

  private val playPauseButton =
    input(
      id     := "play-pause",
      `type` := "button",
      value  := playPauseLabel
    ).render

  playPauseButton.onclick = (e: dom.Event) => {
    if !playing && timeSlider.value == timeSlider.max then {
      timeSlider.value = timeSlider.min
    }
    playing = !playing
    refresh()
  }

  timeSlider.onmousedown = (e: dom.Event) => {
    timeSliderMouseDown = true
    playing = false
  }
  timeSlider.onmouseup = (e: dom.Event) => timeSliderMouseDown = false
  timeSlider.onmousemove = (e: dom.Event) => if e.target == timeSlider then refresh()

  lazy val render = {
    // cheeky! little side-effect to kick off the animation
    setTimeout()

    div(style := "background-color:green, width: 100%")(
      div(
        div(timeSliderLabel, timeSlider),
        playPauseButton
      ),
      containerDiv
    )
  }

  private def setTimeout(): Int =
    dom.window.setTimeout(() => onTick(), animationRefreshRate.toMillis.toInt)

  private def onTick() = {

    if playing then {
      val currentTime = timeSlider.value.toIntOption.getOrElse(0)
      val newTime     = currentTime + playIncrement
      if newTime >= timeSlider.max.toInt then {
        playing = false
      }
      timeSlider.value = s"${newTime}"

      refresh()
    }
    setTimeout()
  }

}
