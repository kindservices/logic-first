package kind.logic.color

object ConsoleColors {
  // ANSI escape codes for colors
  private val RESET  = "\u001B[0m"
  private val RED    = "\u001B[31m"
  private val GREEN  = "\u001B[32m"
  private val YELLOW = "\u001B[33m"
  private val BLUE   = "\u001B[34m"
  private val PURPLE = "\u001B[35m"
  private val CYAN   = "\u001B[36m"
  private val WHITE  = "\u001B[37m"

  // Colored output methods
  def red(text: String): String    = s"$RED$text$RESET"
  def green(text: String): String  = s"$GREEN$text$RESET"
  def yellow(text: String): String = s"$YELLOW$text$RESET"
  def blue(text: String): String   = s"$BLUE$text$RESET"
  def purple(text: String): String = s"$PURPLE$text$RESET"
  def cyan(text: String): String   = s"$CYAN$text$RESET"
  def white(text: String): String  = s"$WHITE$text$RESET"
}
