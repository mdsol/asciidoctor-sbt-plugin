lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    AsciiDoctor / asciiDocBackend := "html",
    AsciiDoctor / asciiDocDoctype := Some("article"),
    AsciiDoctor / asciiDocAttributes := Map("stylesheet" -> "my-theme.css"),
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
