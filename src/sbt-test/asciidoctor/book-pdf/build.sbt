lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    asciiDocBackend := "pdf",
    asciiDocType := Some("book"),
    asciiDocAttributes := Map("toc" -> "left", "source-highlighter" -> "coderay", "stylesheet" -> "my-theme.css"),
    asciiDocDirectory := baseDirectory.value / "src" / "main" / "doc",
    asciiDocOutputDirectory := target.value / "docs",
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
