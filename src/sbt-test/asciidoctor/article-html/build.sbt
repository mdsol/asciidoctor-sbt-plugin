lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    AsciiDoctor / backend := "html",
    AsciiDoctor / doctype := Some("article"),
    AsciiDoctor / attributes := Map("stylesheet" -> "my-theme.css"),
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
