package com.mdsol.sbt.exts

trait ExtensionRegistry {
  def register(extensionClassName: String, blockName: Option[String]): Unit
}
