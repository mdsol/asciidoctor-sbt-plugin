sonatypeProfileName := "com.mdsol"
publishMavenStyle := true
licenses := Seq("MDSOL" -> url("https://github.com/mdsol/asciidoctor-sbt-plugin/blob/master/LICENSE.txt"))

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("austek", "sbt-asciidoctor", "austek@mdsol.com"))

publishTo := sonatypePublishTo.value
releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseVersionBump := sbtrelease.Version.Bump.Bugfix

import sbtrelease.ReleaseStateTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
credentials += Credentials(
  "GnuPG Key ID",
  "gpg",
  "B47AF57CF7DD40EE6B141A627D16F4CEB1F2979C",
  "ignored"
)
