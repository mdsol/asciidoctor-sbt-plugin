package com.asciidoctor.sbt

trait AsciiDocResult

case object Skipped extends AsciiDocResult

case object Success extends AsciiDocResult
