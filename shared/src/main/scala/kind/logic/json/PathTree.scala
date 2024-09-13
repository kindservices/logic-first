package kind.logic.json

import kind.logic.*
import ujson.Value
import upickle.default.*

import scala.collection.immutable

/** This represents a basic file-system-like tree structure, where each node can have data
  *
  * @param children
  *   the child nodes for this node
  * @param data
  *   the data at this node
  */
case class PathTree(children: Map[String, PathTree] = Map.empty, data: Json = ujson.Null)
    derives ReadWriter {

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

  /** @param path
    *   the path to the given part in the tree
    * @param filter
    *   the text filter
    * @return
    *   the children at the given path which match the optional filter
    */
  def query(path: Seq[String], filter: Filter = Filter.Pass): Seq[Json] = {
    for
      node  <- at(path).toSeq
      child <- node.children.values
      data  <- Option(child.data)
      if filter.test(data)
    yield data
  }

  def pretty(prefix: Seq[String] = Nil): Seq[String] = {
    val thisNode =
      if !data.isNull || isLeaf then Seq(prefix.mkString("", ".", s" = ${data.toString()}"))
      else Nil

    val kids = children.flatMap { case (key, child) =>
      child.pretty(prefix :+ key)
    }
    thisNode ++ kids
  }

  /** @param prefix
    *   the prefix path
    * @return
    *   the leaves of the tree
    */
  def leaves(prefix: Seq[String] = Nil): Seq[PathTree.Leaf] = {
    if isLeaf then {
      Seq(PathTree.Leaf(prefix, this))
    } else {
      children.flatMap { case (id, kid) =>
        kid.leaves(prefix :+ id)
      }.toSeq
    }
  }

  /** remove the child at the given path
    * @param path
    *   the path
    * @return
    *   a new tree if found, or None if not found
    */
  def remove(path: Seq[String]): Option[PathTree] = {
    path match {
      case Seq() => None
      case Seq(id) =>
        children.get(id).map(_ => copy(children = children - id))
      case head +: theRest =>
        children.get(head) match {
          case Some(kid) =>
            kid.remove(theRest).map { replaced =>
              copy(children = children.updated(head, replaced))
            }
          case None => None
        }
    }
  }

  def at(
      path: Seq[String]
  ): Option[PathTree] = {
    path match {
      case Seq()           => Option(this)
      case head +: theRest => children.get(head).flatMap(_.at(theRest))
    }
  }

  /** Consider the tree:
    *
    * /data/1/v0/fizz /data/1/v1/buzz /data/2/v0/alpha /data/2/v1/beta /data/2/v2/gamma
    * /data/3/v0/dave
    *
    * for path /data, we want 'latest' to trim: /data/1/v1/buzz /data/2/v2/gamma /data/3/v0/dave
    *
    * @return
    *   the latest tree node leaves
    */
  def latest(
      path: Seq[String]
  )(using ord: Ordering[String] = Ordering.String): immutable.Iterable[PathTree.Leaf] = {
    latestNodes(path)(using ord).flatMap { leaf =>
      leaf.node.leaves(leaf.path)
    }
  }

  /** Consider the tree:
    *
    * /data/1/v0/fizz /data/1/v1/buzz /data/2/v0/alpha /data/2/v1/beta /data/2/v2/gamma
    * /data/3/v0/dave
    *
    * for path /data, we want 'latestNodes' will return /data/1/v1 /data/2/v2 /data/3/v0
    *
    * @return
    *   the latest version of the nodes one-level deep under the given path
    */
  def latestNodes(
      path: Seq[String]
  )(using ord: Ordering[String] = Ordering.String): immutable.Iterable[PathTree.Leaf] = {
    val child: PathTree = at(path).getOrElse(PathTree.empty)
    child.children.collect {
      case (id, versions) if versions.children.nonEmpty =>
        val sortedKids = versions.children.keySet.toList.sorted(using ord)
        val lastKey    = sortedKids.last
        val data       = versions.children(lastKey)
        PathTree.Leaf(path :+ id :+ lastKey, data)
    }
  }

  def patchData(
      path: Seq[String],
      newData: Json,
      createIfNotFound: Boolean = true
  ) = {
    update(path, (t => t.copy(data = t.data.mergeWith(newData))), createIfNotFound)
  }

  def add(
      path: Seq[String],
      newData: Json = ujson.Null
  ): PathTree = updateData(path, newData, true)

  def updateData(
      path: Seq[String],
      newData: Json = ujson.Null,
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

  extension (nodes: immutable.Iterable[Leaf]) {
    def filter(filter: Filter): immutable.Iterable[Leaf] = {
      nodes.filter(n => filter.test(n.data))
    }
  }

  case class Leaf(path: Seq[String], node: PathTree) {
    def data                      = node.data
    override def toString         = pretty()
    def pretty(sep: String = ".") = path.mkString("", sep, s" = ${node.data.render(0)}")
  }

  def apply(data: Json) = new PathTree(data = data)

  def empty                 = forPath("")
  def forPath(path: String) = PathTree().updateData(path.asPath, ujson.Null, true)
}
