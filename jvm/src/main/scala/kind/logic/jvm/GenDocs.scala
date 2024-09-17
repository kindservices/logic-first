package kind.logic.jvm

import eie.io.{_, given}

import scala.language.implicitConversions

case class Scenario[A](
    title: String,
    input: A,
    genMermaid: A => String,
    subDir: String = "",
    preText: String = "",
    postText: String = ""
) {

  def fileName = title.filter(_.isLetterOrDigit)
  def path = {
    subDir match {
      case s"/$p/" => s"$p/$fileName.md"
      case s"$p/"  => s"$p/$fileName.md"
      case s"/$p"  => s"$p/$fileName.md"
      case ""      => s"$fileName.md"
      case p       => s"$p/$fileName.md"
    }
  }
  def mermaidIndented = genMermaid(input).linesIterator.mkString("\n\t|", "\n\t|", "\n")
  def asDoc = s"""
                 |## $title
                 |
                 |When given:
                 |```scala
                 |$input
                 |```
                 |$preText
                 |This is what will happen:    
                 |$mermaidIndented
                 |$postText
   """.stripMargin('|')
}

object GenDocs {

  def apply(scenarios: Seq[Scenario[?]]) = {
    write(scenarios)
    s"./docs/scenarios.md".asPath.text = scenarioMD(scenarios)
  }

  def write(scenarios: Seq[Scenario[?]]) = {
    scenarios.foreach { s =>
      s"./docs/${s.path}".asPath.text = s.asDoc
    }
  }

  def scenarioMD(scenarios: Seq[Scenario[?]]) = {
    val header =
      """# Scenarios
        |This file was generated using [GenDocs](../jrm/src/test/scala/mermaid/GenDocs.scala)
        |
        |To regenerate, run:
        |```sh
        |sbt test:run
        |```
        |""".stripMargin('|')

    scenarios
      .map { s =>
        s" * [${s.title}](${s.path})\n"
      }
      .mkString(header, "\n", "")
  }
}
