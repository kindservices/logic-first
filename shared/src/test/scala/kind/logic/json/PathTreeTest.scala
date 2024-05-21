package kind.logic.json

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class PathTreeTest extends AnyWordSpec with Matchers {
  "PathTree.update" should {
    "update the tree" in {
      val data = ujson.read("""{
        "some" : "value"
      }""")

      val tree    = PathTree.forPath("a/b/c")
      val updated = tree.updateData(Seq("a", "b2", "see"), data, true)
      tree.formatted.shouldBe("a.b.c = null")
      updated.formatted.shouldBe("""a.b.c = null
            |a.b2.see = {"some":"value"}""".stripMargin('|'))
    }

  }
}
