package com.mdsol.sbt.log

import java.io.File

import com.mdsol.sbt.PluginLogger
import org.asciidoctor.log.{LogRecord, Severity, LogHandler => AsciiDocLogHandler}

import scala.collection.mutable.ListBuffer

class MemoryLogHandler(outputToConsole: Boolean, sourceDirectory: File) extends AsciiDocLogHandler with PluginLogger {
  private val records: ListBuffer[LogRecord] = ListBuffer.empty

  override def log(logRecord: LogRecord): Unit = {
    records += logRecord
    if (outputToConsole) logInfo(LogRecordHelper.format(logRecord, sourceDirectory))
  }

  def clear(): Unit =
    records.clear()

  /**
    * Returns LogRecords that are equal or above the severity level.
    *
    * @param severity Asciidoctor severity level
    * @return list of filtered logRecords
    */
  def filter(severity: Severity): List[LogRecord] = records.filter(_.getSeverity.compareTo(severity) >= 0).toList

  /**
    * Returns LogRecords whose message contains text.
    *
    * @param text text to search for in the LogRecords
    * @return list of filtered logRecords
    */
  def filter(text: String): List[LogRecord] = records.filter(_.getMessage.contains(text)).toList

  /**
    * Returns LogRecords that are equal or above the severity level and whose message contains text.
    *
    * @param severity Asciidoctor severity level
    * @param text     text to search for in the LogRecords
    * @return list of filtered logRecords
    */
  def filter(severity: Severity, text: String): List[LogRecord] =
    records.filter(r => r.getSeverity.compareTo(severity) >= 0 && r.getMessage.contains(text)).toList
}
