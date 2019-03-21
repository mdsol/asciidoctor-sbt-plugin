import sbt.Def

import scala.io.Source

lazy val asciiDocVersion: String = sys.props.get("asciidoctorj.versio") match {
  case Some(x) => x
  case _       => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

val check: TaskKey[Unit] = taskKey[Unit]("Test result")

lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  javacOptions ++= Seq(
    "-encoding",
    "UTF-8"
  ),
  scalaVersion := "2.12.8",
  version := "0.1",
  AsciiDoctor / sourceDirectory := baseDirectory.value / "src" / "main" / "doc",
  AsciiDoctor / outputDirectory := target.value / "docs",
  AsciiDoctor / backend := "html",
  AsciiDoctor / headerFooter := false,
  TaskKey[Unit]("prepareDocs") := {
    streams.value.log.info(s"Processing Ascii Docs for ${name.value}")
    (compile in Compile).value
    (processAsciiDoc in AsciiDoctor).value
  }
)

lazy val `asciidoctor-project-1` = (project in file("asciidoctor-project-1"))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    name := "asciidoctor-project-1",
    description := "Runs asciidoctor-sbt-plugin:processAsciiDoc",
    commonSettings
  )

lazy val `asciidoctor-project-2` = (project in file("asciidoctor-project-2"))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    name := "asciidoctor-project-2",
    description := "Runs asciidoctor-sbt-plugin:processAsciiDoc",
    commonSettings
  )

lazy val `asciidoctor-project-3` = (project in file("asciidoctor-project-3"))
  .enablePlugins(AsciiDoctorPlugin)
  .settings(
    name := "asciidoctor-project-3",
    description := "Runs asciidoctor-sbt-plugin:processAsciiDoc",
    commonSettings
  )

lazy val `spi-registered-log` = (project in file("."))
  .settings(
    description := "Tests SPI registration of an AsciidoctorJ LogHandler",
    check := {
      val log = sLog.value
       (1 to 3)
        .map { i =>
          val projectName = s"asciidoctor-project-$i"
          val file = new File(baseDirectory.value, s"$projectName/target/docs/sample.html")
          log.info(s"Checking for existence of $file")
          if (!file.isFile) {
            sys.error("Missing file " + file)
          }
          // validate that asciidoctor.attributes are processed
          val bufferedSource = Source.fromFile(file)
          val text = bufferedSource.getLines.mkString("\n")
          bufferedSource.close
          (projectName, text)
        }.toList.combinations(2).toList
        .collect {
          case a :: b :: Nil if a._2 != b._2 =>
            s"The content of two files are different.\n\n${a._1} : ${b._1}"
        }
        .map { result =>
          sys.error(result)
        }

    }
  )
  .aggregate(
    `asciidoctor-project-1`,
    `asciidoctor-project-2`,
    `asciidoctor-project-3`
  )
