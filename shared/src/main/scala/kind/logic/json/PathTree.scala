package kind.logic.json

import kind.logic._
import ujson.Value
import upickle.default._

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

  /** delete children beyond the given depth
    * @param depth
    *   the depth to which we should prune the tree
    * @return
    *   a tree truncated to the given depth
    */
  def prune(depth: Int): PathTree = {
    depth match {
      case 0 => PathTree(data = data)
      case n => copy(children = children.view.mapValues(_.prune(n - 1)).toMap)
    }
  }

  def isLeaf = children.isEmpty

  /** @param depth
    *   if negative, this will keep data at the leaf nodes. Otherwise only data at the given depth
    *   will be retained
    * @return
    *   a new tree which only has data at the given depth
    */
  def withDataAtDepth(depth: Int): PathTree = {
    def newChildren = children.view.mapValues(_.withDataAtDepth(depth - 1)).toMap
    depth match {
      case 0 => copy(children = newChildren, data = data)
      case _ => copy(children = newChildren, data = ujson.Null)
    }
  }

  /** Drops all data nodes except for the leaves
    * @return
    *   a new PathTree with data only at the leaves
    */
  def withDataAtLeaves: PathTree = {
    if isLeaf then this
    else {
      copy(children = children.view.mapValues(_.withDataAtLeaves).toMap, data = ujson.Null)
    }
  }

  /** @return
    *   the full serialised representation of this tree, where the 'children' and 'data' elements
    */
  def asJson: Json = writeJs(this)

  /** @return
    *   a json representation with
    */
  def collapse: Json = children.foldLeft(data) { case (result, (key, child)) =>
    child.collapse.withKey(key).asUJson.mergeWith(result)
  }

  def formatted: String = pretty().mkString("\n")

  def pretty(prefix: Seq[String] = Nil): Seq[String] = {
    val thisNode =
      if !data.isNull || isLeaf then Seq(prefix.mkString("", ".", s" = ${data.toString()}"))
      else Nil

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

  def keys                    = children.keySet
  def keysSorted: Seq[String] = keys.toSeq.sorted
}

object PathTree {

  def apply(data: Json) = new PathTree(data = data)

  def empty                 = forPath("")
  def forPath(path: String) = PathTree().updateData(path.asPath, ujson.Null, true)
}
