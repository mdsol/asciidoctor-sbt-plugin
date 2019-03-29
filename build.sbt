lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "org.asciidoctor",
    name := "sbt-asciidoctor",
    description := "AsciiDoctor SBT Plugin",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "org.asciidoctor" % "asciidoctorj" % "1.6.1",
      "com.beachape" %% "enumeratum" % "1.5.13"
    ),
    scriptedLaunchOpts ++= Seq(s"-Dplugin.version=${version.value}"),
    scriptedBufferLog := false,
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("releases"),
    javacOptions ++= Seq("-encoding", "UTF-8"),
    scalacOptions := Seq(
      "-encoding",
      "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.8",
      "-language:_",
      "-Xlint",
      "-Xlog-reflective-calls",
      "-Ywarn-adapted-args",
      "-Ywarn-unused",
      "-Ywarn-unused-import"
    )
  )
