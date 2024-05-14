package kind.logic.js

package object table {
  type Json = ujson.Value

  type JPath = Seq[String]

  def columnsForMap(map: Map[String, Json]): Seq[JPath] = {
    map.values.headOption match {
      case Some(json) =>
        json.flattenPaths()
      case None =>
        Nil
    }
  }

  extension (jason: Json) {
    def flattenPaths(prefix: JPath = Nil, paths: Seq[JPath] = Nil): Seq[JPath] = {
      jason match {
        case ujson.Obj(props) =>
          //   props.flatMap { case (key, value) =>
          //     value.flattenPaths(prefix :+ key, paths)
          //   }
          ???
        case _ => paths
      }
    }
  }
}
