package kind.logic

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.*

class MinFPTest extends AnyWordSpec with Matchers {

  "extension asResultTraced" should {
    "work for operations which throw an exception" in {
      val resultTraced = sys.error("Bang").asResultTraced(Container.person("some", "test"), "test")

      val Left(result) = resultTraced.task.either.execOrThrow(): @unchecked
      result.getMessage shouldBe "Bang"
    }
    "error when we try to us it for ZIO types" in {
      val Left(result) = ZIO
        .attempt(1)
        .asResultTraced(Container.person("some", "test"), "test")
        .task
        .either
        .execOrThrow(): @unchecked

      result.getMessage shouldBe "BUG: You're pimping a ZIO type rather than an action. use .taskAsResult or .taskAsResultTraced"
    }
  }
}
