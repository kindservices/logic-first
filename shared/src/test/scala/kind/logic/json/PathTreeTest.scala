package kind.logic.json

import kind.logic.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.immutable
import scala.collection.immutable.Map

class PathTreeTest extends AnyWordSpec with Matchers {
  val data = Map("some" -> "value").asUJson

  val deepTree = PathTree.empty
    .updateData("grandparent/parent/child1".asPath, Map("id" -> "a").asUJson)
    .updateData("grandparent/parent/child2".asPath, Map("id" -> "b").asUJson)
    .updateData("grandparent".asPath, Map("grandparent-data" -> "hello").asUJson)
    .updateData("grandparent/parent".asPath, Map("parent-data" -> "world").asUJson)

  "PathTree.latest" should {
    "return the latest leaves under a grandparent node" in {

      val original: PathTree = PathTree
        .forPath("data/1/v0/fizz")
        .add("data/1/v1/buzz".asPath)
        .add("data/2/v0/alpha".asPath)
        .add("data/2/v1/beta".asPath)
        .add("data/2/v2/gamma".asPath)
        .add("data/3/v0/dave".asPath)

      val latest: immutable.Iterable[PathTree.Leaf] = original.latest("data".asPath)
      latest.mkString("\n") shouldBe """data.1.v1.buzz = null
                                       |data.2.v2.gamma = null
                                       |data.3.v0.dave = null""".stripMargin
    }
  }
  "PathTree.remove" should {
    "remove data at the given path" in {
      val original =
        PathTree
          .forPath("a/b/c/d")
          .add("a/b/c/e".asPath)
          .add("a/b/c2".asPath)
          .add("a2".asPath)

      val Some(removed) = original.remove("a/b/c".asPath)
      removed.collapse shouldBe """{
                                  |  "a": {
                                  |    "b": {
                                  |      "c2": null
                                  |    }
                                  |  },
                                  |  "a2": null
                                  |}
                                  |""".stripMargin.parseAsJson

      original.remove("Dave".asPath) shouldBe None
      original.remove("a/b/c/d/e/f".asPath) shouldBe None
    }
  }
  "PathTree.query" should {
    "return the results matching the given filter at the path" in {
      val records @ List(a, b, c, d) = List(
        Map("id" -> "a", "tag" -> "foo").asUJson,
        Map("id" -> "b", "tag" -> "bar").asUJson,
        Map("id" -> "c", "tag" -> "baz").asUJson,
        Map("id" -> "d", "tag" -> "foo").asUJson
      )
      val differentPath = Map("id" -> "e", "tag" -> "foo, bar, baz").asUJson

      val root = PathTree.empty
        .updateData("db/anotherId/record1".asPath, differentPath)

      val fullTree = records.zipWithIndex.foldLeft(root) { case (tree, (data, i)) =>
        tree.updateData(s"db/someId/record$i".asPath, data)
      }

      fullTree
        .query(
          "db/someId".asPath,
          Filter.Contains("tag", "foo")
        ) should contain theSameElementsAs (List(a, d))
      fullTree.query("db/someId".asPath, Filter.Contains("tag", "bar")) should contain only (b)
      fullTree.query("db/someId".asPath, Filter.Contains("tag", "ba")) should contain only (b, c)

      fullTree.query(
        "db/anotherId".asPath,
        Filter.Contains("tag", "foo")
      ) should contain only (differentPath)
      fullTree.query("db/anotherId".asPath, Filter.Contains("tag", "nope")) shouldBe (empty)
      fullTree.query("db/anotherId".asPath, Filter.Pass) should contain only (differentPath)
    }
  }

  "PathTree.formatted" should {
    "show a squashed view of the tree" in {
      deepTree.formatted shouldBe ("""grandparent = {"grandparent-data":"hello"}
                                     |grandparent.parent = {"parent-data":"world"}
                                     |grandparent.parent.child1 = {"id":"a"}
                                     |grandparent.parent.child2 = {"id":"b"}""".stripMargin)
    }
  }

  "PathTree.collapse" should {
    "not include the 'children' or 'data' nodes" in {
      deepTree.collapse shouldBe """{
                                        |  "grandparent": {
                                        |    "parent": {
                                        |      "parent-data": "world",
                                        |      "child2": {
                                        |        "id": "b"
                                        |      },
                                        |      "child1": {
                                        |        "id": "a"
                                        |      }
                                        |    },
                                        |    "grandparent-data": "hello"
                                        |  }
                                        |}""".stripMargin.parseAsJson
    }
  }
  "PathTree.withDataAtLeaves" should {

    "only keep leaf data if given -1" in {
      deepTree
        .updateData("grandparent/anotherParent".asPath, Map("id" -> "c").asUJson)
        .withDataAtLeaves
        .collapse shouldBe """{
                                                    |  "grandparent": {
                                                    |    "parent": {
                                                    |      "child2": {
                                                    |        "id": "b"
                                                    |      },
                                                    |      "child1": {
                                                    |        "id": "a"
                                                    |      }
                                                    |    },
                                                    |    "anotherParent": {
                                                    |      "id": "c"
                                                    |    }
                                                    |  }
                                                    |}""".stripMargin.parseAsJson
    }
  }
  "PathTree.withDataAtDepth" should {
    "only keep data at depth 1" in {
      deepTree.withDataAtDepth(1).collapse shouldBe """{
                                                      |  "grandparent": {
                                                      |    "parent": {
                                                      |      "child2": null,
                                                      |      "child1": null
                                                      |    },
                                                      |    "grandparent-data": "hello"
                                                      |  }
                                                      |}""".stripMargin.parseAsJson
    }
    "only keep data at depth 2" in {
      deepTree.withDataAtDepth(2).collapse shouldBe """{
                                                      |  "grandparent": {
                                                      |    "parent": {
                                                      |      "parent-data": "world",
                                                      |      "child2": null,
                                                      |      "child1": null
                                                      |    }
                                                      |  }
                                                      |}""".stripMargin.parseAsJson
    }
  }
  "PathTree.prune" should {
    "prune children" in {
      deepTree.prune(1).collapse shouldBe """{
                                              |  "grandparent": {
                                              |    "grandparent-data": "hello"
                                              |  }
                                              |}""".stripMargin.parseAsJson
      deepTree.prune(2).collapse shouldBe
        """
          |{
          |  "grandparent": {
          |    "parent": {
          |      "parent-data": "world"
          |    },
          |    "grandparent-data": "hello"
          |  }
          |}
          |""".stripMargin.parseAsJson
    }
  }
  "PathTree.forPath" should {
    "return an empty tree for ''" in {
      val tree = PathTree.forPath("")
      tree.children shouldBe (empty)
    }
    "return an empty tree for '/'" in {
      val tree = PathTree.forPath("/")
      tree.children shouldBe (empty)
    }
    "return a tree with a single child for '/a'" in {
      val tree = PathTree.forPath("/a")
      tree.children.keySet shouldBe Set("a")
    }
    "return a tree with a single child for 'a'" in {
      val tree = PathTree.forPath("a")
      tree.children.keySet shouldBe Set("a")
    }
  }
  "PathTree.update" should {
    "update the tree" in {
      val tree    = PathTree.forPath("a/b/c")
      val updated = tree.updateData(Seq("a", "b2", "see"), data, true)
      tree.formatted.shouldBe("a.b.c = null")
      updated.formatted.shouldBe("""a.b.c = null
            |a.b2.see = {"some":"value"}""".stripMargin('|'))
    }

  }
}
