import scala.io.Source

lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    AsciiDoctor / backend := "html",
    AsciiDoctor / doctype := Some("article"),
    AsciiDoctor / enableVerbose := true,
    AsciiDoctor / asciiDocRequires := List("asciidoctor-diagram"),
    AsciiDoctor / attributes := Map("toc" -> "left"),
    AsciiDoctor / sourceDirectory := baseDirectory.value / "src" / "main" / "doc",
    AsciiDoctor / outputDirectory := target.value / "docs",
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1",
    TaskKey[Unit]("check") := {
      val log = sLog.value
      val expectedFiles = Seq("sample.html", "images/asciidoctor-diagram-process.png", "images/auth-protocol.png", "images/dot-example.svg")

      expectedFiles.foreach {
        expectedFile =>
          val file = new File((AsciiDoctor / outputDirectory).value, expectedFile)
          log.info(s"Checking for existence of $file")
          if (!file.isFile) {
            sys.error("Missing file " + file)
          }

          if (expectedFile == "sample.html") {
            val bufferedSource = Source.fromFile(file)
            val text = bufferedSource.getLines.mkString
            val contentToVerify = Seq(
              "images/asciidoctor-diagram-process.png",
              """alt="asciidoctor diagram process"""",
              "images/auth-protocol.png",
              """alt="auth protocol"""",
              "images/dot-example.svg",
              """alt="dot example""""
            )
            val result = contentToVerify.filterNot(text.contains)
            bufferedSource.close
            if (result.nonEmpty) {
              sys.error("Diagrams not processed, HTML not found for :" + result)
            }
          }
      }
    }
  )
