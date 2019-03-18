package com.asciidoctor.sbt

import sbt.Keys.streams

trait PluginLogger {
  protected def logDebug(msg: String): Unit = {
    streams.value.log.debug(s"[sbt-asciidoctor] $msg")
  }

  protected def logInfo(msg: String): Unit = {
    streams.value.log.info(s"[sbt-asciidoctor] $msg")
  }

  protected def logError(msg: String): Unit = {
    streams.value.log.error(s"[sbt-asciidoctor] $msg")
  }

  protected def logWarn(msg: String): Unit = {
    streams.value.log.warn(s"[sbt-asciidoctor] $msg")
  }
}
