lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    asciiDocBackend := "html",
    asciiDocDirectory := baseDirectory.value / "src" / "main" / "doc",
    asciiDocOutputFile := Some(new File("a_path/custom-filename.html")),
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
