package kind.logic.color

/** Functions for creating colors
  */
object Colors:
  import scala.math._

  def namedColors: LazyList[String] = (
    names.to(LazyList) #::: lightNames.to(LazyList) #::: namedColors
  )

  def names = Seq(
    "green",
    "blue",
    // "orange",
    // "red",
    "purple",
    "gray",
    "brown"
    // "pink"
  )
  def lightNames: Seq[String] = {
    Seq(
      "lightgreen",
      "lightblue",
      "lightcoral",
      "lightcyan",
      "lightgoldenrodyellow",
      "lightgray",
      "lightpink",
      "lightsalmon",
      "lightseagreen",
      "lightskyblue",
      "lightslategray",
      "lightsteelblue",
      "lightyellow"
    )
  }

  /** Generate a color palette with N distinct colors using the HSL color wheel.
    *
    * @param numColors
    *   The number of distinct colors required (N).
    * @return
    *   An array of colors in hexadecimal format.
    */
  def apply(numColors: Int, lightness: Int = 90, saturation: Double = 70): Seq[String] = {
    require(numColors > 0 && numColors <= 20, "N must be between 1 and 20")

    // Generate colors by varying the hue across 360 degrees
    (0 until numColors).map { i =>
      val hue = (360.0 / numColors) * i // Evenly spaced hues

      // Convert HSL to RGB and then to hexadecimal
      hslToHex(hue, saturation, lightness)
    }
  }

  /** Convert HSL values to hexadecimal RGB color.
    *
    * @param hue
    *   Hue value (0-360).
    * @param saturation
    *   Saturation percentage (0-100).
    * @param lightness
    *   Lightness percentage (0-100).
    * @return
    *   Hexadecimal RGB color as a string.
    */
  def hslToHex(hue: Double, saturation: Double, lightness: Double): String = {
    val c         = (1 - abs(2 * lightness / 100 - 1)) * (saturation / 100)
    val x         = c * (1 - abs((hue / 60) % 2 - 1))
    val m: Double = lightness / 100 - c / 2

    val (r1: Double, g1: Double, b1: Double) = hue match {
      case h if h < 60  => (c, x, 0.0)
      case h if h < 120 => (x, c, 0.0)
      case h if h < 180 => (0.0, c, x)
      case h if h < 240 => (0.0, x, c)
      case h if h < 300 => (x, 0.0, c)
      case _            => (c, 0.0, x)
    }: @unchecked

    val (r, g, b) = ((r1 + m) * 255, (g1 + m) * 255, (b1 + m) * 255)
    f"#${r.toInt}%02x${g.toInt}%02x${b.toInt}%02x"
  }

end Colors
