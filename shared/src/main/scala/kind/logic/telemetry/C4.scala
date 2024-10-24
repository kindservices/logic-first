package kind.logic.telemetry

import kind.logic.color.Colors
import kind.logic.{Container, ContainerType}
import kind.logic.json.*

import scala.collection.immutable

object C4 {

  case class ElementStyle(bgColor: String, color: String)

  case class Style(
      style: String = C4.DefaultStyle,
      colorMap: Map[String, C4.ElementStyle] = Map.empty,
      layoutByName: Map[String, String] = Map.empty,
      defaultSystemLayout: String = "autolayout lr",
      defaultContainerLayout: String = "autolayout lr"
  )

  extension (text: String) {
    def asPerson    = s"${text.asIdentifier}Person"
    def asSystem    = s"${text.asIdentifier}System"
    def asContainer = s"${text.asIdentifier}Container"
    def asComponent = s"${text.asIdentifier}Component"
  }

  /** This class just groups the call data by the softwareSystem 'name' to produce the software
    * system block:
    *
    * {{{
    *            fooSystem = softwareSystem "Foo" {
    *              webAppComponent = container "Web App" {
    *                operatorPerson -> this "createDraft"
    *              }
    *
    *              restServiceComponent = container "BFF" {
    *                webAppComponent -> this "createDraft"
    *              }
    *
    *              databaseComponent = container "DB" {
    *                restServiceComponent -> this "saveDraft"
    *              }
    *            }
    * }}}
    *
    * @param name
    *   the software system name
    * @param callsIntoThisSystem
    *   the calls which are into this system
    * @param callFromThisSystem
    *   the calls made which originate in this system
    * @param originalComponentsInScope
    *   the components viewable at this moment as we sequentially create systems
    */
  private case class SoftwareSystem(
      name: String,
      callsIntoThisSystem: Seq[CompletedCall],
      callFromThisSystem: Seq[CompletedCall]
  ) {
    private def distinctBySource(calls: Seq[CompletedCall]) =
      calls.foldLeft(Vector[CompletedCall]()) {
        case (seq, next)
            if seq.exists(c => c.source == next.source && c.operation == next.operation) =>
          seq
        case (seq, next) => seq :+ next
      }
    private def distinctByTarget(calls: Seq[CompletedCall]) =
      calls.foldLeft(Vector[CompletedCall]()) {
        case (seq, next)
            if seq.exists(c => c.target == next.target && c.operation == next.operation) =>
          seq
        case (seq, next) => seq :+ next
      }

    // a map keyed by the source container containing unique calls to targets
    private val outgoingContainerCalls: Map[Container, Seq[CompletedCall]] =
      callFromThisSystem.groupBy(_.source).view.mapValues(distinctByTarget).toMap
    // a map keyed by the target container containing unique calls into this container
    private val incomingContainerCalls: Map[Container, Seq[CompletedCall]] =
      callsIntoThisSystem.groupBy(_.target).view.mapValues(distinctBySource).toMap

    private val containers = outgoingContainerCalls.keySet ++ incomingContainerCalls.keySet

    def asC4: String = {

      val declarations = containers.map { container =>
        s"""
           |             ${container.label.asContainer} = container "${container.label}" {
           |               tags "${container.`type`}" "${container.label}"
           |             }
           |""".stripMargin
      }

      declarations.mkString(
        s"""    ${name.asSystem} = softwareSystem "${name}" {
           |       tags "${name}"
           |\n""".stripMargin,
        "\n",
        s"\n    }"
      )
    }
  }

  val DefaultStyle =
    """
      |            element "Element" {
      |                color #ffffff
      |            }
      |            element "Person" {
      |                background #05527d
      |                shape person
      |            }
      |            element "Software System" {
      |                background #066296
      |            }
      |            element "Script" {
      |                shape diamond
      |            }
      |            element "Job" {
      |                shape circle
      |            }
      |            element "FileSystem" {
      |                shape box
      |            }
      |            element "DesktopApp" {
      |                shape hexagon
      |            }
      |            element "WebApp" {
      |                shape hexagon
      |            }
      |            element "MobileApp" {
      |                shape hexagon
      |            }
      |            element "Container" {
      |                background #0773af
      |            }
      |            element "Function" {
      |                shape diamond
      |            }
      |            element "Service" {
      |                shape roundedbox
      |            }
      |            element "Queue" {
      |                shape ellipse
      |            }
      |            element "Queue" {
      |                shape cylinder
      |            }
      |            element "Database" {
      |                shape cylinder
      |            }
      |""".stripMargin

}

/** https://c4model.com/abstractions/container
  *
  * @param calls
  */
case class C4(calls: Seq[CompletedCall]) {

  import C4.*

  def diagram(settings: C4.Style = C4.Style()): String = {
    s"""
       |workspace {
       |    model {
       |        // ================================================
       |        // Users
       |        // ================================================
       |        ${users}
       |
       |        // ================================================
       |        // Software Systems
       |        // ================================================
       |        ${softwareSystems}
       |
       |        // ================================================
       |        // Interactions
       |        // ================================================
       |        ${interactions}
       |    }
       |
       |    views {
       |        ${views(settings)}
       |
       |        theme default
       |
       |         styles {
       |         ${settings.style}
       |
       |${colors(settings.colorMap)}
       |        }
       |    }
       |
       |    configuration {
       |        scope none
       |    }
       |}
       |""".stripMargin
  }

  private def people =
    calls.filter(_.source.`type` == ContainerType.Person).map(_.source.label).distinct

  /** Example:
    * {{{
    *         user = person "User"
    *         issuer = person "Issuer"
    *         operator = person "Operator"
    * }}}
    * @return
    *   a string block containing the users in this system
    */
  private def users: String = people
    .map { name =>
      s"""        ${name.asPerson} = person "${name}""""
    }
    .mkString("\n")

  private def systems: Seq[SoftwareSystem] = {
    val callsByTargetSystem: Map[String, Seq[CompletedCall]] =
      calls.groupBy(_.action.target.softwareSystem)

    val callsBySourceSystem = calls.groupBy(_.action.source.softwareSystem)

    val allCalls = callsByTargetSystem.keySet ++ callsBySourceSystem.keySet

    allCalls.map { name =>
      val callsIntoTarget = callsByTargetSystem.getOrElse(name, Nil)
      val bySource        = callsBySourceSystem.getOrElse(name, Nil)
      SoftwareSystem(name, callsIntoTarget, bySource)
    }.toSeq
  }

  /** {{{
    *           fooSystem = softwareSystem "Foo" {
    *             webAppComponent = container "Web App" {
    *               operatorPerson -> this "createDraft"
    *             }
    *
    *             restServiceComponent = container "BFF" {
    *               webAppComponent -> this "createDraft"
    *             }
    *
    *             databaseComponent = container "DB" {
    *               restServiceComponent -> this "saveDraft"
    *             }
    *           }
    * }}}
    * @return
    *   a string block of the software systems
    */
  private def softwareSystems: String = {
    systems.map(_.asC4).mkString("\n")
  }

  private def interactions = {
    val operations = calls.groupBy(c => (c.source, c.target)).map { case ((from, to), srcToCalls) =>
      val operations = srcToCalls.distinct match {
        case Seq(only) => only.operation
        case many =>
          val ops = many.map(_.operation)
          ops.init.mkString("", ", ", s" and ${many.last.operation}")
      }
      s"""        ${from.label.asContainer} -> ${to.label.asContainer} "${operations}" """
    }

    operations.mkString("\n", "\n", "")
  }

  private def views(style: C4.Style): String = {
    import style.*
    val view = systems.map { s =>
      s"""        systemContext ${s.name.asSystem} "${s.name}" {
          |            include *
          |            ${layoutByName.getOrElse(s.name, defaultSystemLayout)}
          |        }
          |
          |        container ${s.name.asSystem} {
          |            include *
          |            ${layoutByName.getOrElse(s.name, defaultContainerLayout)}
          |        }
          |""".stripMargin
    }

    view.mkString("\n", "\n", "")
  }

  private def colors(colorMap: Map[String, C4.ElementStyle]) = {
    systems
      .zip(Colors(systems.size))
      .map { case (s, color) =>
        val bg = colorMap.get(s.name).map(_.bgColor).getOrElse(color)
        val fg = colorMap.get(s.name).map(_.color).getOrElse("#000000")

        s"""
           |            element "${s.name}" {
           |                 background "$bg"
           |                 color "$fg"
           |            }
           |""".stripMargin
      }
      .mkString("\n")
  }
}
