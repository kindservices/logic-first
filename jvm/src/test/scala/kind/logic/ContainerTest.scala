package kind.logic

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import kind.logic.*
import kind.logic.json.*
import upickle.default.*
import zio.*

object ContainerTest {

  def get(using enclosingScope: sourcecode.Enclosing) = enclosingScope.value


  object nested {
    object innerObj {
      def example = get
    }

    trait API {
      def foo = get
    }

    val instance = (new API {}).foo
  }
}
class ContainerTest extends AnyWordSpec with Matchers {

  def inTest = ContainerTest.get

  def nestedFunc : String = {
    def foo = ContainerTest.get

    foo
  }

  "Container.systemAndContainer" should {
    /** this is handy just to see what Enclosing does for various approaches:
     *
     * as it turns out, the text can be split on whitespace, a hash, or a dot, depending on being in an object, inner function, etc:
     *
     * kind.logic.ContainerTest.nested.innerObj.example
     * kind.logic.ContainerTest.nested.API#foo
     * kind.logic.ContainerTest#inTest
     * kind.logic.ContainerTest#nestedFunc foo
     */
    "have different values" ignore {
      println(ContainerTest.nested.innerObj.example)
      println(ContainerTest.nested.instance)
      println(inTest)
      println(nestedFunc)

    }
    "use the last two elements from a path" in {
      Container.systemAndContainer(using sourcecode.Enclosing("")) shouldBe ("", "")
      Container.systemAndContainer(using sourcecode.Enclosing("foo")) shouldBe ("", "foo")
      Container.systemAndContainer(using sourcecode.Enclosing("a.b.c")) shouldBe ("b", "c")
      Container.systemAndContainer(using sourcecode.Enclosing("kind.logic.ContainerTest.nested.API#foo")) shouldBe ("API", "foo")
      Container.systemAndContainer(using sourcecode.Enclosing("kind.logic.ContainerTest#nestedFunc foo")) shouldBe ("nestedFunc", "foo")
    }
  }
}
