package com.asciidoctor.sbt

sealed case class AsciiDoctorException(msg: String, throwable: Throwable) extends Exception(msg, throwable) {
  def this(msg: String) = this(msg, null)
  def this() = this(null, null)
}

case object AsciiDoctorEmptyResourcesException extends AsciiDoctorException

final case class JRubyRuntimeContext(override val msg: String, override val throwable: Throwable) extends AsciiDoctorException

final case class AsciiDoctorIssuesFoundException(override val msg: String) extends AsciiDoctorException
