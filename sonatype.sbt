useGpg := false
usePgpKeyHex("87558ab01f3201fc")
pgpPublicRing := baseDirectory.value / "project" / ".gnupg" / "pubring.asc"
pgpSecretRing := baseDirectory.value / "project" / ".gnupg" / "secring.asc"
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)
sonatypeProfileName := "com.mdsol"
publishMavenStyle := true
licenses := Seq("MDSOL" -> url("https://github.com/austek/asciidoctor-sbt-plugin/blob/master/LICENSE.txt"))

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