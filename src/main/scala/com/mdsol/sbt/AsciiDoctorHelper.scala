package com.mdsol.sbt

import org.asciidoctor.{Attributes, AttributesBuilder}

object AsciiDoctorHelper {
  def addAttributes(attributes: Map[String, Any], attributesBuilder: AttributesBuilder): Unit = {
    // TODO Figure out how to reliably set other values (like boolean values, dates, times, etc)
    attributes.foreach { case (attribute, value) => addAttribute(attribute, value, attributesBuilder) }
  }

  def addAttribute(attribute: String, value: Any, attributesBuilder: AttributesBuilder): AttributesBuilder = {
    value match {
      case null | "true" => attributesBuilder.attribute(attribute, "")
      case "false"       => attributesBuilder.attribute(attribute, null)
      case bool: Boolean => attributesBuilder.attribute(attribute, Attributes.toAsciidoctorFlag(bool))
      case _             => attributesBuilder.attribute(attribute, value)
    }
  }
}
