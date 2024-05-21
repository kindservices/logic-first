package kind.logic.js.db

import upickle.default.*

/** This represents a basic file-system-like tree structure, where each node can have data
  *
  * @param children
  *   the child nodes for this node
  * @param data
  *   the data at this node
  */
case class PathTree(children: Map[String, PathTree] = Map.empty, data: Json = ujson.Null)
    derives ReadWriter {
  def add(child: String, data: Json = ujson.Null) = {
    val newChild = PathTree(data = data)
    copy(children = children + (child -> newChild))
  }

  def asJson = writeJs(this)

  def formatted = pretty().mkString("\n")

  def pretty(prefix: Seq[String] = Nil): Seq[String] = {
    val thisNode =
      if children.isEmpty then Seq(prefix.mkString("", ".", s" = ${data.toString()}")) else Nil

    val kids = children.flatMap { case (key, child) =>
      child.pretty(prefix :+ key)
    }
    thisNode ++ kids
  }

  def at(
      path: Seq[String]
  ): Option[PathTree] = {
    path match {
      case Seq()           => Option(this)
      case head +: theRest => children.get(head).flatMap(_.at(theRest))
    }
  }

  def patchData(
      path: Seq[String],
      newData: Json,
      createIfNotFound: Boolean = true
  ) = {
    update(path, (t => t.copy(data = t.data.mergeWith(newData))), createIfNotFound)
  }

  def updateData(
      path: Seq[String],
      newData: Json,
      createIfNotFound: Boolean = true
  ) = update(path, (_.copy(data = newData)), createIfNotFound)

  def update(
      path: Seq[String],
      f: PathTree => PathTree,
      createIfNotFound: Boolean = true
  ): PathTree = {
    path match {
      case Seq() => f(this)
      case head +: theRest =>
        children.get(head) match {
          case None if createIfNotFound =>
            copy(children = children + (head -> PathTree().update(theRest, f, createIfNotFound)))
          case None => this
          case Some(child) =>
            copy(children = children.updated(head, child.update(theRest, f, createIfNotFound)))
        }
    }
  }

  def keys     = children.keySet
  def keysList = keys.toSeq.sorted
}

object PathTree {

  extension (path: String) {
    def asPath = path.split("/").toSeq
  }

  def apply(data: Json) = new PathTree(data = data)

  def forPath(path: String) = PathTree().updateData(path.asPath, ujson.Null, true)
}
