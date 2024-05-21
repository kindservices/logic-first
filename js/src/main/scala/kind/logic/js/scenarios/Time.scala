package kind.logic.js.scenarios

import org.scalajs.dom.window

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import concurrent.duration._

object Time:

  private def parseDate(d8: String) = d8.toUpperCase match {
    case s"$year-$month-${date}T${hr}:$min:$sec.${millis}Z[$zoneName]" =>
      val zone = ZoneId.of(zoneName)
      ZonedDateTime.of(
        year.toInt,
        month.toInt,
        date.toInt,
        hr.toInt,
        min.toInt,
        sec.toInt,
        millis.toInt * 1000000,
        zone
      )
    case other =>
      window.console.log("other: " + other)
      ZonedDateTime.parse(other, DateTimeFormatter.ISO_ZONED_DATE_TIME)
  }

  def parseTime(value: String) = value.toLowerCase.trim match {
    case "now"     => ZonedDateTime.now(ZoneId.of("UTC"))
    case s"t+${x}" => ZonedDateTime.now(ZoneId.of("UTC")).plusSeconds(x.toInt)
    case s"t-${x}" => ZonedDateTime.now(ZoneId.of("UTC")).minusSeconds(x.toInt)
    case time =>
      try {
        parseDate(time)
      } catch {
        case e =>
          window.alert(e.getMessage)
          ZonedDateTime.now(ZoneId.of("UTC"))
      }
  }

  def parseDuration(value: String): FiniteDuration = value.toLowerCase.trim match {
    case s"${x}s"   => x.toInt.seconds
    case s"${x}m"   => x.toInt.minutes
    case s"${x}min" => x.toInt.minutes
    case s"${x}ms"  => x.toInt.millis
    case s"${x}us"  => x.toInt.micros
    case s"${x}h"   => x.toInt.hours
    case s"${x}hs"  => x.toInt.hours
    case s"${x}hrs" => x.toInt.hours
    case other =>
      window.alert(s"Couldn't parse '$other' as a duration")
      0.seconds
  }
