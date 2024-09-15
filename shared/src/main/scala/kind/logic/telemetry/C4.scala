package kind.logic.telemetry

import zio.UIO

/**
 * https://c4model.com/abstractions/container
 *
 * Here's an example of what we're going for:
 *
 * {{{
 *
 *   workspace {
 *
 *     model {
 *         // ================================================
 *         // Users
 *         // ================================================
 *         user = person "User"
 *
 *         issuer = person "Issuer"
 *         operator = person "Operator"
 *
 *         // ================================================
 *         // Contexts
 *         // ================================================
 *         assets = softwareSystem "Assets" {
 *             docExchange = container "Document Exchange"
 *             onboardingService = container "Onboarding"
 *             dmiPortalAssets = container "DMI Portal (Assets)" {
 *                 issuer -> this "Creates Draft Assets"
 *                 operator -> this "Reviews/Approves Assets"
 *             }
 *
 *             assetIdentity = container "Asset Identity" {
 *
 *             }
 *
 *             assetsDatabase = container "Database" {
 *                 onboardingService -> this "Writes Asset Drafts"
 *             }
 *
 *             iTouch = container "iTouch" {
 *                 docExchange -> this "Uploads documents"
 *             }
 *
 *             securitiesGrid = container "Securities Grid" {
 *                 onboardingService -> this "Creates Assets"
 *             }
 *         }
 *
 *         marketplace = softwareSystem "Marketplace" {
 *
 *         }
 *
 *         settlement = softwareSystem "Settlement"
 *
 *         custody = softwareSystem "Custody"
 *
 *         dlt = softwareSystem "DLT" {
 *             securitiesGrid -> this "Mints Tolkens"
 *         }
 *
 *         payments = softwareSystem "Payments"
 *
 *         softwareSystem = softwareSystem "Software System"
 *
 *         // ================================================
 *         // Interactions between Users and Contexts
 *         // ================================================
 *
 *
 *         // ================================================
 *         // Relationships within Assets
 *         // ================================================
 *         dmiPortalAssets -> docExchange -> "Uploads Documents"
 *         dmiPortalAssets -> onboardingService -> "Saves Drafts"
 *     }
 *
 *     views {
 *         systemContext assets "Assets" {
 *             include *
 *             autolayout lr
 *         }
 *
 *         systemContext marketplace "Marketplace" {
 *             include *
 *             autolayout lr
 *         }
 *
 *         container marketplace {
 *             include *
 *             autolayout lr
 *         }
 *
 *         container assets {
 *             include *
 *             autolayout lr
 *         }
 *
 *         theme default
 *     }
 *
 *     configuration {
 *         scope softwaresystem
 *     }
 *
 * }
 *
 * }}}
 *
 * @param calls
 */
class C4(calls: UIO[Seq[CompletedCall]]) {

  def diagram : String = {
    s"""
       |workspace {
       |    model {
       |        // ================================================
       |        // Users
       |        // ================================================
       |        ${users}
       |
       |        // ================================================
       |        // Contexts
       |        // ================================================
       |        ${softwareSystems}
       |
       |        ${interactions}
       |    }
       |
       |    views {
       |        ${views}
       |
       |        theme default
       |    }
       |
       |    configuration {
       |        scope softwaresystem
       |    }
       |}
       |""".stripMargin
  }

  def users = {
    s"""
       |        user = person "User"
       |
       |        issuer = person "Issuer"
       |        operator = person "Operator"
       |""".stripMargin
  }
  def softwareSystems =
    s"""
       |        assets = softwareSystem "Assets" {
       |            docExchange = container "Document Exchange"
       |            onboardingService = container "Onboarding"
       |            dmiPortalAssets = container "DMI Portal (Assets)" {
       |                issuer -> this "Creates Draft Assets"
       |                operator -> this "Reviews/Approves Assets"
       |            }
       |
       |            assetIdentity = container "Asset Identity" {
       |
       |            }
       |
       |            assetsDatabase = container "Database" {
       |                onboardingService -> this "Writes Asset Drafts"
       |            }
       |
       |            iTouch = container "iTouch" {
       |                docExchange -> this "Uploads documents"
       |            }
       |
       |            securitiesGrid = container "Securities Grid" {
       |                onboardingService -> this "Creates Assets"
       |            }
       |        }
       |
       |        marketplace = softwareSystem "Marketplace" {
       |
       |        }
       |
       |        settlement = softwareSystem "Settlement"
       |
       |        custody = softwareSystem "Custody"
       |
       |        dlt = softwareSystem "DLT" {
       |            securitiesGrid -> this "Mints Tolkens"
       |        }
       |
       |        payments = softwareSystem "Payments"
       |
       |        softwareSystem = softwareSystem "Software System"
       |""".stripMargin

  def interactions = {
    s"""
       |        // ================================================
       |        // Interactions between Users and Contexts
       |        // ================================================
       |
       |        // ================================================
       |        // Relationships within Assets
       |        // ================================================
       |        dmiPortalAssets -> docExchange -> "Uploads Documents"
       |        dmiPortalAssets -> onboardingService -> "Saves Drafts"
       |""".stripMargin
  }
  def views = {
    s"""
       |
       |        systemContext assets "Assets" {
       |            include *
       |            autolayout lr
       |        }
       |
       |        systemContext marketplace "Marketplace" {
       |            include *
       |            autolayout lr
       |        }
       |
       |        container marketplace {
       |            include *
       |            autolayout lr
       |        }
       |
       |        container assets {
       |            include *
       |            autolayout lr
       |        }
       |""".stripMargin
  }
}
