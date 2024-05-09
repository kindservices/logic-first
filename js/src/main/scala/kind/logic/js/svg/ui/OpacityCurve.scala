package kind.logic.js.svg.ui

import scala.math._

/** This object provides a utility function for converting a percentage into a normal distribution
  * curve.
  *
  * It's used to conver the percentage journey (0.0 to 1.0) into an opacity, where the ends (start
  * and end) are 0.0, and the middle is 1.0.
  */
object OpacityCurve:

  /** Converts a percentage (0.0 to 1.0) into a normal distribution curve with a specified standard
    * deviation.
    *
    * @param percent
    *   The percentage (0.0 to 1.0) to be converted.
    * @param stdDev
    *   The standard deviation of the desired normal distribution.
    * @return
    *   The value derived from the normal distribution curve.
    */
  def apply(percent: Double, clip: Double, stdDev: Double = 0.4): Double = {
    require(percent >= 0.0 && percent <= 1.0, "Percentage must be between 0.0 and 1.0")

    if percent <= clip || percent >= 1 - clip then return 0.0
    else {

      val mean = 0.5

      // Convert the percentage to a z-score (standard score)
      val zScore = (percent - mean) / stdDev

      // Calculate the normal distribution's PDF
      // Use a scaling factor to normalize the result within a given range
      exp(-0.5 * pow(zScore, 2)) / (stdDev * sqrt(2 * Pi))
    }

  }
