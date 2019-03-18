package com.asciidoctor.sbt.log

import java.io.{File, IOException}

import org.asciidoctor.log.LogRecord

object LogRecordHelper {
  // includes replacers for cursor (file & line)
  val ASCIIDOCTOR_FULL_LOG_FORMAT = "asciidoctor: %s: %s: line %s: %s"
  val ASCIIDOCTOR_SIMPLE_LOG_FORMAT = "asciidoctor: %s: %s"

  /**
    * Formats the logRecord in a similar manner to original Asciidoctor.
    * Note: prints the absolute path of the file.
    *
    * @param logRecord Asciidoctor logRecord to format
    * @return Asciidoctor-like formatted string
    */
  def format(logRecord: LogRecord): String = {
    val cursor = logRecord.getCursor
    String.format(ASCIIDOCTOR_FULL_LOG_FORMAT, logRecord.getSeverity, cursor.getFile, cursor.getLineNumber, logRecord.getMessage)
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
    var relativePath = ""
    try if (cursor != null) relativePath = new File(cursor.getFile).getCanonicalPath.substring(sourceDirectory.getCanonicalPath.length + 1)
    catch {
      case e: IOException =>
        // use the absolute path as fail-safe
        relativePath = cursor.getFile
    }
    if (relativePath.length > 0) String.format(ASCIIDOCTOR_FULL_LOG_FORMAT, logRecord.getSeverity, relativePath, cursor.getLineNumber, logRecord.getMessage)
    else String.format(ASCIIDOCTOR_SIMPLE_LOG_FORMAT, logRecord.getSeverity, logRecord.getMessage)
  }
}
