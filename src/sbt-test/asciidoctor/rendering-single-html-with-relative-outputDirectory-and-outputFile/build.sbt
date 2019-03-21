lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    AsciiDoctor / backend := "html",
    AsciiDoctor / sourceDirectory := baseDirectory.value / "src" / "main" / "doc",
    AsciiDoctor / outputDirectory := target.value / "output_path",
    AsciiDoctor / outputFile := Some(new File("a_path/custom-filename.html")),
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
