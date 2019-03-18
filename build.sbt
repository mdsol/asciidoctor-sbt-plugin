lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "asciidoctor-sbt-plugin",
    scalaVersion := "2.12.8",
    Compile / scalacOptions += "-Xlint",
    Compile / console / scalacOptions --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
    libraryDependencies ++= Seq(
      "org.asciidoctor" % "asciidoctorj" % "1.6.1",
      "com.beachape" %% "enumeratum" % "1.5.13"
    )
  )
