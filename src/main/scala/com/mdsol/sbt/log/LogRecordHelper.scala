package com.mdsol.sbt.log

import java.io.{File, IOException}

import org.asciidoctor.log.LogRecord

object LogRecordHelper {

  /**
    * Formats the logRecord in a similar manner to original Asciidoctor.
    * Note: prints the absolute path of the file.
    *
    * @param logRecord Asciidoctor logRecord to format
    * @return Asciidoctor-like formatted string
    */
  def format(logRecord: LogRecord): String = {
    val cursor = logRecord.getCursor
    s"asciidoctor: ${logRecord.getSeverity}: ${cursor.getFile}: line ${cursor.getLineNumber}: ${logRecord.getMessage}"
  }

  /**
    * Formats the logRecord in a similar manner to original Asciidoctor.
    * Note: prints the relative path of the file to `sourceDirectory`.
    *
    * @param logRecord       Asciidoctor logRecord to format
    * @param sourceDirectory source directory of the converted AsciiDoc document
    * @return Asciidoctor-like formatted string
    */
  def format(logRecord: LogRecord, sourceDirectory: File): String = {
    val cursor = logRecord.getCursor
    val relativePath: String =
      try {
        if (cursor != null) {
          new File(cursor.getFile).getCanonicalPath.substring(sourceDirectory.getCanonicalPath.length + 1)
        } else ""
      } catch {
        case _: IOException =>
          // use the absolute path as fail-safe
          cursor.getFile
      }
    if (relativePath.length > 0) {
      s"asciidoctor: ${logRecord.getSeverity}: $relativePath: line ${cursor.getLineNumber}: ${logRecord.getMessage}"
    } else {
      s"asciidoctor: ${logRecord.getSeverity}: ${logRecord.getMessage}"
    }
  }
}
