package com.mdsol.sbt

import enumeratum.values._

import scala.collection.immutable

sealed abstract class AttributeUndefined(val value: String) extends StringEnumEntry

case object AttributeUndefined extends StringEnum[AttributeUndefined] {

  case object Drop extends AttributeUndefined("drop")
  case object DropLine extends AttributeUndefined("drop-line")

  val values: immutable.IndexedSeq[AttributeUndefined] = findValues

}
