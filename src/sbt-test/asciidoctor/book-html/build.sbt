lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    AsciiDoctor / asciiDocBackend := "html",
    AsciiDoctor / asciiDocDoctype := Some("book"),
    AsciiDoctor / asciiDocAttributes := Map("toc" -> "left", "source-highlighter" -> "coderay", "stylesheet" -> "my-theme.css"),
    AsciiDoctor / asciiDocSourceDirectory := baseDirectory.value / "src" / "main" / "doc",
    AsciiDoctor / asciiDocOutputDirectory := target.value / "docs",
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
