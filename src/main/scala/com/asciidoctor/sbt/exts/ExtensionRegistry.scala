package com.asciidoctor.sbt.exts

trait ExtensionRegistry {
  def register(extensionClassName: String, blockName: Option[String]): Unit
}
