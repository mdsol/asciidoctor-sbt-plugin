package com.mdsol.sbt.log

import org.asciidoctor.log.Severity

case class FailIf(severity: Option[Severity], containsText: Option[String])
