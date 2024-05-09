package kind.examples

import zio.*
import scala.concurrent.duration.*

/** This package is an example of more non-trivial examples.
  *
  * A restaurant which will automatically re-order ingredients when they run low, and a marketplace
  * which will get the best price from some suppliers.
  *
  * This shows what more advanced logic and more advanced interpreters look like.
  *
  * By using 'RunnableProgram' with Telemetry, ideally the learning curve is reduced to a function
  * for each operation, which has to return a Result[A] (Note: We could perhaps improve those
  * ergonomics from having to go Task[A] -> Result[A], but perhaps that's a bridge too far )
  *
  * Motivation: This example shows that developers don't have to write multiple interpreters (which
  * themselves will duplicate logic ). This hopefully lowers both the effort and the learning curve
  * a bit, at the cost of knowing about ZIO, and our 'Telemetry' and 'RunnableProgram'
  *
  * Next TODO: Create a top-level app which composes the Restaurant and the Marketplace, and creates
  * a Mermaid Sequence Diagram from the result
  */
package object trade {}
