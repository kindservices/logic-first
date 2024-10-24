package kind.logic

import kind.logic.telemetry.Telemetry

package object dsl {

  /** A DSL for tracing a block of code:
    *
    * {{{
    *
    *  given ThisService = Container.service
    *  given DB = Container.db("Postgres")
    *
    *  def foo(x: Int) : Task[Boolean] = call(DB).withArgs(x) {
    *     ... some code
    *     true
    *  }
    * }}}
    */
  def call(target: Container, operationName: String = null)(using
      source: Container,
      telemetry: Telemetry,
      scope: sourcecode.Enclosing
  ): dsl.Call = dsl.Call(target, operationName)
}
