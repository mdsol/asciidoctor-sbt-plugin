import scala.io.Source

lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    asciiDocBackend := "html",
    asciiDocType := Some("article"),
    asciiDocAttributes := Map("toc" -> "left", "source-highlighter" -> "coderay"),
    asciiDocDirectory := baseDirectory.value / "src" / "main" / "doc",
    asciiDocOutputDirectory := target.value / "docs",
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1",
    TaskKey[Unit]("check") := {
      val log = sLog.value
      val expectedFiles = Seq("sample.html")

      expectedFiles.foreach { expectedFile =>
        val file = new File((asciiDocOutputDirectory).value, expectedFile)
        log.info(s"Checking for existence of $file")
        if (!file.isFile) {
          sys.error("Missing file " + file)
        }

        // validate that asciidoctor.attributes are processed
        val bufferedSource = Source.fromFile(file)
        val text = bufferedSource.getLines.mkString
        if (!text.contains("""<body class="article toc2 toc-left">""") || !text.contains("""<pre class="CodeRay highlight">""")) {
          sys.error("Attributes not processed")
        }
        bufferedSource.close

      }
    }
  )
