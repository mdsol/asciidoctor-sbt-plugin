package com.asciidoctor.sbt.extensions

trait ExtensionRegistry {
  def register(extensionClassName: String, blockName: String): Unit
}
