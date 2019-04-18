package com.mdsol.sbt

sealed class AsciiDoctorException(msg: String, throwable: Throwable) extends Exception(msg, throwable) {
  def this(msg: String) = this(msg, null)
  def this() = this(null, null)
}

case object AsciiDoctorEmptyResourcesException extends AsciiDoctorException

final case class JRubyRuntimeContextException(msg: String, throwable: Throwable) extends AsciiDoctorException(msg, throwable)

final case class AsciiDoctorIssuesFoundException(msg: String) extends AsciiDoctorException(msg)
