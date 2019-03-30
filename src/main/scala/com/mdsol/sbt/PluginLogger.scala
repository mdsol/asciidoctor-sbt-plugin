package com.mdsol.sbt

import sbt.Def
import sbt.Keys.streams

trait PluginLogger {
  protected def logDebug(msg: String): Unit = {
    Def.task {
      streams.value.log.debug(s"[sbt-asciidoctor] $msg")
    }
  }

  protected def logInfo(msg: String): Unit = {
    Def.task {
      streams.value.log.info(s"[sbt-asciidoctor] $msg")
    }
  }

  protected def logError(msg: String): Unit = {
    Def.task {
      streams.value.log.error(s"[sbt-asciidoctor] $msg")
    }
  }

  protected def logWarn(msg: String): Unit = {
    Def.task {
      streams.value.log.warn(s"[sbt-asciidoctor] $msg")
    }
  }
}
