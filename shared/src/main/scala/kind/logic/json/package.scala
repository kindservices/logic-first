package kind.logic

import ujson._

import util.control.NonFatal

package object json {
  type Json = ujson.Value

  type JKey  = String | Int
  type JPath = Seq[JKey]

  extension (path: String) {
    def asPath: Seq[String] = path.split("/").toSeq.filterNot(_.isEmpty)

    def parseAsJson: Json = ujson.read(path)
    def asJsonString: Json = ujson.Str(path)
  }

  def diffValues(a: ujson.Value, b: ujson.Value): ujson.Value = (a, b) match {
    case (ujson.Obj(aMap), ujson.Obj(bMap)) =>
      val keys = aMap.keySet ++ bMap.keySet
      val diffs = keys.flatMap { key =>
        (aMap.get(key), bMap.get(key)) match {
          case (Some(aValue), Some(bValue)) =>
            val diff = diffValues(aValue, bValue)
            if diff == ujson.Obj() then None else Some(key -> diff)
          case (Some(aValue), None) =>
            Some(key -> ujson.Obj("removed" -> aValue))
          case (None, Some(bValue)) =>
            Some(key -> ujson.Obj("added" -> bValue))
          case (None, None) => None // This should never happen
        }
      }.toMap
      ujson.Obj.from(diffs)

    case (ujson.Arr(aArray), ujson.Arr(bArray)) =>
      val diffs = aArray
        .zipAll(bArray, ujson.Null, ujson.Null)
        .zipWithIndex
        .flatMap { case ((aValue, bValue), idx) =>
          val diff = diffValues(aValue, bValue)
          if diff == ujson.Obj() then None else Some(idx.toString -> diff)
        }
        .toMap
      ujson.Obj.from(diffs)

    case (aValue, bValue) if aValue == bValue =>
      ujson.Obj() // No difference

    case (aValue, bValue) =>
      ujson.Obj("from" -> aValue, "to" -> bValue)
  }

  extension (json: Json) {

    def diffWith(other: ujson.Value) = diffValues(json, other)

    def mergeWith(other: ujson.Value): ujson.Value = (json, other) match {
      case (ujson.Obj(aMap), ujson.Obj(bMap)) =>
        val mergedMap: scala.collection.mutable.Map[String, Value] = aMap ++ bMap.map {
          case (k, v) =>
            k -> (aMap.get(k) match {
              case Some(aValue) => aValue.mergeWith(v)
              case None         => v
            })
        }
        ujson.Obj.from(mergedMap)

      case (ujson.Arr(aArray), ujson.Arr(bArray)) =>
        ujson.Arr(aArray ++ bArray)

      case (aValue, ujson.Null) => aValue
      case (_, bValue)          => bValue
    }

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
