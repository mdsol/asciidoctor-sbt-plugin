lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    AsciiDoctor / asciiDocBackend := "html",
    AsciiDoctor / asciiDocSourceDirectory := baseDirectory.value / "src" / "main" / "doc",
    AsciiDoctor / asciiDocOutputFile := Some(new File("a_path/custom-filename.html")),
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
