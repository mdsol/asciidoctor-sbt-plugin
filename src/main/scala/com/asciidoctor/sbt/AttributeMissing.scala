package com.asciidoctor.sbt

import enumeratum.values._

import scala.collection.immutable

sealed abstract class AttributeMissing(val value: String) extends StringEnumEntry

case object AttributeMissing extends StringEnum[AttributeMissing] {

  case object Skip extends AttributeMissing("skip")
  case object Drop extends AttributeMissing("drop")
  case object DropLine extends AttributeMissing("drop-line")

  val values: immutable.IndexedSeq[AttributeMissing] = findValues

}
