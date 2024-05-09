import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / name := "logic-first"
ThisBuild / organization := "com.github.aaronp"
// version := "0.0.1"
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / scalafmtOnCompile := true
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// Common settings
lazy val commonSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "kind.logic.buildinfo",
  // this is our model libraries, generated from the service.yaml and created/publised via 'make packageRestCode'
  // libraryDependencies += "com.github.aaronp" %%% "contract" % "0.0.3",
  libraryDependencies ++= Seq(
    "dev.zio" %%% "zio" % "2.0.22",
    "org.scalatest" %%% "scalatest" % "3.2.18" % Test,
    "com.lihaoyi" %%% "upickle" % "3.2.0",
    "com.lihaoyi" %%% "sourcecode" % "0.4.1"
  )
)

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-rewrite",
  "-new-syntax"
)

lazy val app = crossProject(JSPlatform, JVMPlatform).in(file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(commonSettings).
  jvmSettings(
    name := "logic-first-jvm",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "cask" % "0.9.2",
      "com.github.aaronp" %% "eie" % "2.0.1")
//      "com.github.aaronp" %% "eie" % "2.0.1" % Test)
  ).
  jsSettings(
    name := "logic-first-js",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0",
      "com.lihaoyi" %%% "scalatags" % "0.12.0",
      "org.scala-js" %%% "scalajs-dom" % "2.4.0"
    ),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("livechart")))
    },
  )

lazy val root = project.in(file(".")).
  aggregate(app.js, app.jvm).
  settings(
    publish := {},
    publishLocal := {},
  )


ThisBuild / publishMavenStyle := true

val githubUser = "kindservices"
val githubRepo = "logic-first"
ThisBuild / publishTo := Some("GitHub Package Registry" at s"https://maven.pkg.github.com/$githubUser/$githubRepo")

sys.env.get("GITHUB_TOKEN") match {
  case Some(token) if token.nonEmpty =>
    ThisBuild / credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      githubUser,
      token
    )
  case _ =>
    println("\n\t\tGITHUB_TOKEN not set - assuming a local build\n\n")
    credentials ++= Nil
}


releasePublishArtifactsAction := PgpKeys.publishSigned.value

assembly / test := {}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

// see https://leonard.io/blog/2017/01/an-in-depth-guide-to-deploying-to-maven-central/
pomIncludeRepository := (_ => false)

// To sync with Maven central, you need to supply the following information:
Global / pomExtra := {
  <url>https://github.com/kindservices/logic-first</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>
          aaronp
        </id>
        <name>Aaron Pritzlaff</name>
        <url>https://github.com/kindservices/logic-first
        </url>
      </developer>
    </developers>
}
