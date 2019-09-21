sonatypeProfileName := "com.mdsol"
publishMavenStyle := true
licenses := Seq("MDSOL" -> url("https://github.com/mdsol/asciidoctor-sbt-plugin/blob/master/LICENSE.txt"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/mdsol/mauth-java-client"),
    "scm:git@github.com:mdsol/mauth-java-client.git"
  )
)

developers := List(
  Developer(id = "austek", name = "Ali Ustek", email = "austek@mdsol.com", url = url("https://github.com/austek"))
)

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("austek", "sbt-asciidoctor", "austek@mdsol.com"))

publishTo := sonatypePublishToBundle.value
releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseVersionBump := sbtrelease.Version.Bump.Bugfix

releaseCrossBuild := false // true if you cross-build the project for multiple Scala versions
import sbtrelease.ReleaseStateTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For cross-build projects, use releaseStepCommand("+publishSigned")
  releaseStepCommandAndRemaining("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
credentials += Credentials(
  "GnuPG Key ID",
  "gpg",
  "B47AF57CF7DD40EE6B141A627D16F4CEB1F2979C",
  "ignored"
)
