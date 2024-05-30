package kind.logic

import upickle.default._

/** Useful for providing messages/detail back to front-end services in a consistent way
  * @param message
  *   the user message
  * @param success
  *   a success flag
  * @param warning
  *   an optional warning message
  * @param error
  *   an optional error message
  */
case class ActionResult(message: String, success: Boolean, warning: String = "", error: String = "")
    derives ReadWriter {
  def withData[A: ReadWriter](data: A): Json = {
    data.withKey("data").merge(this.withKey("result"))
  }
}

object ActionResult {
  def apply(msg: String)               = new ActionResult(msg, true)
  def fail(msg: String)                = new ActionResult(msg, false)
  def fail(msg: String, error: String) = new ActionResult(msg, false, warning = "", error = error)
}
