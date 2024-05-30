package kind.logic.json

import kind.logic.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Map

class PathTreeTest extends AnyWordSpec with Matchers {
  val data = Map("some" -> "value").asUJson

  val deepTree = PathTree.empty
    .updateData("grandparent/parent/child1".asPath, Map("id" -> "a").asUJson)
    .updateData("grandparent/parent/child2".asPath, Map("id" -> "b").asUJson)
    .updateData("grandparent".asPath, Map("grandparent-data" -> "hello").asUJson)
    .updateData("grandparent/parent".asPath, Map("parent-data" -> "world").asUJson)

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
