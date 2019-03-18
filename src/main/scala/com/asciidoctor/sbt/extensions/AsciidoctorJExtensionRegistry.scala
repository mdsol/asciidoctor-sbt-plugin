package com.asciidoctor.sbt.extensions

import java.lang.reflect.Method
import java.util

import org.asciidoctor.Asciidoctor
import org.asciidoctor.extension._

class AsciidoctorJExtensionRegistry(asciidoctorInstance: Asciidoctor) extends ExtensionRegistry {

  private val javaExtensionRegistry: JavaExtensionRegistry = asciidoctorInstance.javaExtensionRegistry

  override def register(extensionClassName: String, blockName: String): Unit = {

    val clazz: Class[_ <: Processor] = try { Class.forName(extensionClassName).asInstanceOf[Class[Processor]] } catch {
      case _: ClassCastException     => throw new RuntimeException("'" + extensionClassName + "' is not a valid AsciidoctorJ processor class")
      case _: ClassNotFoundException => throw new RuntimeException("'" + extensionClassName + "' not found in classpath")
    }

    if (classOf[DocinfoProcessor].isAssignableFrom(clazz)) {
      javaExtensionRegistry.docinfoProcessor(clazz.asInstanceOf[DocinfoProcessor])
    } else if (classOf[Preprocessor].isAssignableFrom(clazz)) {
      javaExtensionRegistry.preprocessor(clazz.asInstanceOf[Preprocessor])
    } else if (classOf[Postprocessor].isAssignableFrom(clazz)) {
      javaExtensionRegistry.postprocessor(clazz.asInstanceOf[Postprocessor])
    } else if (classOf[Treeprocessor].isAssignableFrom(clazz)) {
      javaExtensionRegistry.treeprocessor(clazz.asInstanceOf[Treeprocessor])
    } else if (classOf[BlockProcessor].isAssignableFrom(clazz)) {
      if (blockName == null) {
        javaExtensionRegistry.block(clazz.asInstanceOf[BlockProcessor])
      } else {
        javaExtensionRegistry.block(blockName, clazz.asInstanceOf[BlockProcessor])
      }
    } else if (classOf[IncludeProcessor].isAssignableFrom(clazz)) {
      javaExtensionRegistry.includeProcessor(clazz.asInstanceOf[IncludeProcessor])
    } else if (classOf[BlockMacroProcessor].isAssignableFrom(clazz)) {
      if (blockName == null) {
        javaExtensionRegistry.blockMacro(clazz.asInstanceOf[BlockMacroProcessor])
      } else {
        javaExtensionRegistry.blockMacro(blockName, clazz.asInstanceOf[BlockMacroProcessor])
      }
    } else if (classOf[InlineMacroProcessor].isAssignableFrom(clazz)) {
      if (blockName == null) {
        javaExtensionRegistry.inlineMacro(clazz.asInstanceOf[InlineMacroProcessor])
      } else {
        javaExtensionRegistry.inlineMacro(blockName, clazz.asInstanceOf[InlineMacroProcessor])
      }
    }
  }

  // TODO: Remove after first successful test
  private def register(target: Any, methodName: String, args: Any*): Unit = {
    for (method <- javaExtensionRegistry.getClass.getMethods) {
      if (isMethodMatching(method, methodName, args: _*)) try {
        method.invoke(target, args)
        return
      } catch {
        case e: Exception =>
          throw new Exception("Unexpected exception while registering extensions", e)
      }
    }
    throw new Exception("Internal Error. Could not register " + methodName + " with arguments " + util.Arrays.asList(args))
  }

  private def isMethodMatching(method: Method, methodName: String, args: Any*): Boolean = {
    if (method.getName != methodName) return false
    if (method.getParameterTypes.length != args.length) return false

    // Don't care for primitives here, there's no method on JavaExtensionRegistry with primitives.
    method.getParameterTypes.zip(args).forall { case (param, arg) => param.isAssignableFrom(arg.getClass) }
  }
}
