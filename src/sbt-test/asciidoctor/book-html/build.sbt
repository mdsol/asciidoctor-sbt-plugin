lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    AsciiDoctor / backend := "html",
    AsciiDoctor / doctype := Some("book"),
    AsciiDoctor / attributes := Map("toc" -> "left", "source-highlighter" -> "coderay", "stylesheet" -> "my-theme.css"),
    AsciiDoctor / sourceDirectory := baseDirectory.value / "src" / "main" / "doc",
    AsciiDoctor / outputDirectory := target.value / "docs",
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
