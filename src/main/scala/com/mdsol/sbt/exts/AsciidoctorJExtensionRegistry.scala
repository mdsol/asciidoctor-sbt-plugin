package com.mdsol.sbt.exts

import org.asciidoctor.Asciidoctor
import org.asciidoctor.extension._

class AsciidoctorJExtensionRegistry(asciidoctorInstance: Asciidoctor) extends ExtensionRegistry {

  private val javaExtensionRegistry: JavaExtensionRegistry = asciidoctorInstance.javaExtensionRegistry

  override def register(extensionClassName: String, blockName: Option[String]): Unit = {

    try {
      val clazz: Class[_ <: Processor] = Class.forName(extensionClassName).asInstanceOf[Class[Processor]]

      clazz match {
        case c if classOf[DocinfoProcessor].isAssignableFrom(c) => javaExtensionRegistry.docinfoProcessor(clazz.asInstanceOf[DocinfoProcessor])
        case c if classOf[Preprocessor].isAssignableFrom(c) => javaExtensionRegistry.preprocessor(clazz.asInstanceOf[Preprocessor])
        case c if classOf[Postprocessor].isAssignableFrom(c) => javaExtensionRegistry.postprocessor(clazz.asInstanceOf[Postprocessor])
        case c if classOf[Treeprocessor].isAssignableFrom(c) => javaExtensionRegistry.treeprocessor(clazz.asInstanceOf[Treeprocessor])
        case c if classOf[IncludeProcessor].isAssignableFrom(c) => javaExtensionRegistry.includeProcessor(clazz.asInstanceOf[IncludeProcessor])
        case c if classOf[BlockProcessor].isAssignableFrom(c) =>
          blockName match {
            case Some(bName) => javaExtensionRegistry.block(bName, clazz.asInstanceOf[BlockProcessor])
            case None => javaExtensionRegistry.block(clazz.asInstanceOf[BlockProcessor])
          }
        case c if classOf[BlockMacroProcessor].isAssignableFrom(c) =>
          blockName match {
            case Some(bName) => javaExtensionRegistry.blockMacro(bName, clazz.asInstanceOf[BlockMacroProcessor])
            case None => javaExtensionRegistry.blockMacro(clazz.asInstanceOf[BlockMacroProcessor])
          }
        case c if classOf[InlineMacroProcessor].isAssignableFrom(c) =>
          blockName match {
            case Some(bName) => javaExtensionRegistry.inlineMacro(bName, clazz.asInstanceOf[InlineMacroProcessor])
            case None => javaExtensionRegistry.inlineMacro(clazz.asInstanceOf[InlineMacroProcessor])
          }
        case _ => throw new RuntimeException(s"Couldn't figure out the type of Processor from class '$clazz'")
      }
    } catch {
      case _: ClassCastException => throw new RuntimeException(s"'$extensionClassName' is not a valid AsciidoctorJ processor class")
      case _: ClassNotFoundException => throw new RuntimeException(s"'$extensionClassName' not found in classpath")
    }
  }
}
