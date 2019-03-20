import scala.io.Source
lazy val root = (project in file("."))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    asciiDocBackend := "html",
    asciiDocDoctype := Some("article"),
    asciiDocAttributes := Map("toc" -> "left", "source-highlighter" -> "coderay"),
    asciiDocSourceDirectory := baseDirectory.value / "src" / "main" / "doc",
    asciiDocOutputDirectory := target.value / "docs",
    name := "simple-doc",
    scalaVersion := "2.12.8",
    version := "0.1",
    TaskKey[Unit]("check") := {
      val expectedFiles = Seq("sample.html")

      expectedFiles.foreach { expectedFile =>
        val file = new File(asciiDocOutputDirectory.value, expectedFile)
        if (!file.isFile) {
          sys.error("Missing file " + file)
        }
        println(s"Checking for existence of $file")

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
