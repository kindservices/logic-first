package kind.logic.js

import ujson.*
import util.control.NonFatal

package object db {
  type Json = ujson.Value

  type JKey  = String | Int
  type JPath = Seq[JKey]

  extension (json: Json) {
    def at(path: String, showError: Boolean = true): Option[Value] = {
      path.split(".", -1).foldLeft(Option(json)) { (acc, key) =>
        acc match {
          case Some(obj: ujson.Obj) => obj.value.get(key)
          case Some(ujson.Arr(values)) =>
            try {
              Option(values(key.toInt))
            } catch {
              case NonFatal(e) =>
                Option(
                  ujson.Str(
                    s"Error: trying to get the value at '$path' for json $json when evaluating key '$key' on $values: $e"
                  )
                ).filter(_ => showError)
            }
          case Some(other) =>
            Option(
              ujson.Str(s"Error: value at $key in '$path' was $other for json $json")
            ).filter(_ => showError)
          case None => None
        }
      }
    }
    def flatten: Map[JPath, Value] = flattenJson(json, Nil)
    def paths: Seq[String]         = flatten.keySet.map(_.mkString(".")).toSeq.sorted
  }

  def flattenJson(json: Json, prefix: Seq[JKey]): Map[JPath, Value] = {
    json match {
      case obj: Obj =>
        obj.value.flatMap { case (k, v) =>
          flattenJson(v, prefix :+ k)
        }.toMap
      case arr: Arr =>
        arr.value.zipWithIndex.flatMap { case (v, i) =>
          flattenJson(v, prefix :+ i)
        }.toMap
      case _ => Map(prefix -> json)
    }
  }

  def flattenJsonStr(json: Json, parentKey: String = ""): Map[String, Value] = {
    json match {
      case obj: Obj =>
        obj.value.flatMap { case (k, v) =>
          val newKey = if parentKey.isEmpty then k else s"$parentKey.$k"
          flattenJsonStr(v, newKey)
        }.toMap

      case arr: Arr =>
        arr.value.zipWithIndex.flatMap { case (v, i) =>
          val newKey = s"$parentKey[$i]"
          flattenJsonStr(v, newKey)
        }.toMap

      case _ =>
        Map(parentKey -> json)
    }
  }
}
