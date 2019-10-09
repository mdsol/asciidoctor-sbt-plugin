lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    asciiDocBackend := "html",
    asciiDocType := Some("article"),
    asciiDocAttributes := Map("stylesheet" -> "my-theme.css"),
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
