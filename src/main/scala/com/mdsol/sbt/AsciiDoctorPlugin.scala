package com.mdsol.sbt

import java.io.IOException
import java.util.logging.Logger

import com.mdsol.sbt.exts.{AsciidoctorJExtensionRegistry, ExtensionConfiguration}
import com.mdsol.sbt.log.{FailIf, LogHandler, LogRecordHelper, MemoryLogHandler}
import org.asciidoctor._
import org.asciidoctor.jruby.internal.JRubyRuntimeContext
import org.asciidoctor.jruby.{AbstractDirectoryWalker, AsciiDocDirectoryWalker}
import org.asciidoctor.log.{LogRecord, Severity}
import org.jruby.Ruby
import sbt.Keys._
import sbt.librarymanagement.Configuration
import sbt.librarymanagement.Configurations.Runtime
import sbt.{Def, _}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.matching.Regex

object AsciiDoctorPlugin extends AutoPlugin with AsciiDoctorPluginLogger {

  object autoImport extends AsciiDoctorPluginKeys
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    asciiDocConvert := processAsciiDocTask.value,
    asciiDocAttributeMissing := AttributeMissing.Skip,
    asciiDocAttributeUndefined := AttributeUndefined.DropLine,
    asciiDocAttributes := Map.empty,
    asciiDocAttributesChain := "",
    asciiDocBackend := "docbook",
    asciiDocCatalogAssets := false,
    asciiDocDirectory := baseDirectory.value / "src" / "docs" / "asciidoc",
    asciiDocERuby := "",
    asciiDocEmbedAssets := false,
    asciiDocEnableVerbose := false,
    asciiDocEncoding := "",
    asciiDocExtensionPattern := raw""".*\\.a((sc(iidoc)?)|d(oc)?)\$$""".r,
    asciiDocExtensions := List.empty,
    asciiDocGemPath := Some(""),
    asciiDocHeaderFooter := true,
    asciiDocImagesDir := Some("images@"), // '@' Allows override by :imagesdir: document attribute
    asciiDocLogHandler := LogHandler(outputToConsole = true, FailIf(Some(Severity.ERROR), None)),
    asciiDocOutputDirectory := target.value / "generated-docs",
    asciiDocOutputFile := None,
    asciiDocPreserveDirectories := false,
    asciiDocRelativeBaseDir := false,
    asciiDocRequires := List.empty,
    asciiDocResources := List.empty,
    asciiDocSourceDocumentExtensions := List.empty,
    asciiDocSourceDocumentName := None,
    asciiDocSourceHighlighter := None,
    asciiDocSourcemap := false,
    asciiDocSynchronizations := List.empty,
    asciiDocTemplateCache := true,
    asciiDocTemplateDir := None,
    asciiDocTemplateEngine := None,
    asciiDocTitle := "",
    asciiDocType := None
  )

  private def processAsciiDocTask: Def.Initialize[Task[AsciiDocResult]] = Def.task[AsciiDocResult] {
    val skp = (skip in publish).value
    val ref = thisProjectRef.value
    if (skp) {
      logDebug(s"Skipping AsciiDoc processing for ${ref.project}")
      Skipped
    }
    if (!asciiDocDirectory.value.exists) {
      logInfo(s"asciiDocDirectory ${asciiDocDirectory.value.getPath} does not exist. Skip processing")
      Skipped
    }

    if (asciiDocResources.value.nonEmpty) {
      asciiDocResources.value.foreach { resource =>
        if (!resource.exists || (resource.isDirectory && resource.list.length == 0)) {
          throw AsciiDoctorEmptyResourcesException
        }
      }
    }

    val asciidoctor = getAsciidoctorInstance(asciiDocGemPath.value)

    if (asciiDocEnableVerbose.value) asciidoctor.requireLibrary("enable_verbose.rb")

    asciidoctor.requireLibraries(asciiDocRequires.value.asJava)

    val optionsBuilder = setOptionsOnBuilder(
      asciiDocBackend.value,
      asciiDocHeaderFooter.value,
      asciiDocERuby.value,
      asciiDocSourcemap.value,
      asciiDocCatalogAssets.value,
      asciiDocTemplateCache.value,
      asciiDocType.value,
      asciiDocTemplateEngine.value,
      asciiDocTemplateDir.value
    )

    val attributesBuilder = setAttributesOnBuilder(
      asciiDocSourceHighlighter.value,
      asciiDocEmbedAssets.value,
      asciiDocImagesDir.value,
      asciiDocAttributeMissing.value,
      asciiDocAttributeUndefined.value,
      asciiDocAttributes.value,
      asciiDocAttributesChain.value
    )

    optionsBuilder.attributes(attributesBuilder)

    val extensionRegistry = new AsciidoctorJExtensionRegistry(asciidoctor)
    asciiDocExtensions.value.foreach { extension =>
      extensionRegistry.register(extension.className, extension.blockName)
    }

    // TODO: implement copyResources

    val sourceFiles: List[File] = asciiDocSourceDocumentName.value match {
      case Some(srcDocName) => List(new File(asciiDocDirectory.value, srcDocName))
      case None => scanSourceFiles(asciiDocDirectory.value, asciiDocSourceDocumentExtensions.value)
    }

    // register LogHandler to capture asciidoctor messages
    val memoryLogHandler = new MemoryLogHandler(asciiDocLogHandler.value.outputToConsole, asciiDocDirectory.value)
    if (sourceFiles.nonEmpty) {
      asciidoctor.registerLogHandler(memoryLogHandler)
      // disable default console output of AsciidoctorJ
      Logger.getLogger("asciidoctor").setUseParentHandlers(false)
    }

    renderFiles(
      asciidoctor,
      memoryLogHandler,
      asciiDocLogHandler.value,
      optionsBuilder,
      asciiDocDirectory.value,
      sourceFiles,
      asciiDocOutputDirectory.value,
      asciiDocOutputFile.value,
      asciiDocRelativeBaseDir.value,
      asciiDocPreserveDirectories.value,
      Set.empty
    )

    synchronize(asciiDocSynchronizations.value)

    Success
  }

  @tailrec
  private def renderFiles(
    asciidoctor: Asciidoctor,
    memoryLogHandler: MemoryLogHandler,
    asciiDocLogHandler: LogHandler,
    optionsBuilder: OptionsBuilder,
    asciiDocDirectory: File,
    sourceFiles: List[File],
    asciiDocOutputDirectory: File,
    asciiDocOutputFile: Option[File],
    asciiDocRelativeBaseDir: Boolean,
    asciiDocPreserveDirectories: Boolean,
    renderedFiles: Set[File]
  ): Set[File] = {
    sourceFiles match {
      case Nil => renderedFiles
      case source :: remainingSourceFiles =>
        val destinationPath =
          setDestinationPaths(
            asciiDocDirectory,
            source,
            asciiDocOutputDirectory,
            asciiDocOutputFile,
            optionsBuilder,
            asciiDocRelativeBaseDir,
            asciiDocPreserveDirectories
          )
        val updatedRenderedFiles = renderedFiles + destinationPath

        if (renderedFiles.size == updatedRenderedFiles.size) {
          logWarn(s"Duplicated destination found: overwriting file: ${destinationPath.getAbsolutePath}")
        }

        if (asciiDocOutputFile.isEmpty)
          renderFile(asciidoctor, optionsBuilder, source)
        else
          renderFile(asciidoctor, optionsBuilder.toFile(destinationPath), source)

        processLogMessages(memoryLogHandler, asciiDocDirectory, asciiDocLogHandler)
        renderFiles(
          asciidoctor,
          memoryLogHandler,
          asciiDocLogHandler,
          optionsBuilder,
          asciiDocDirectory,
          remainingSourceFiles,
          asciiDocOutputDirectory,
          asciiDocOutputFile,
          asciiDocRelativeBaseDir,
          asciiDocPreserveDirectories,
          updatedRenderedFiles
        )
    }
  }

  protected def renderFile(asciidoctor: Asciidoctor, options: OptionsBuilder, file: File): Unit = {
    asciidoctor.convertFile(file, options.asMap)
    logRenderedFile(file)
  }

  private def processLogMessages(memoryLogHandler: MemoryLogHandler, asciiDocDirectory: File, asciiDocLogHandler: LogHandler): Unit = {
    def logRecords(records: List[LogRecord], msg: String): Unit = {
      records.foreach { record =>
        logError(LogRecordHelper.format(record, asciiDocDirectory))
      }
      if (records.nonEmpty) {
        throw AsciiDoctorIssuesFoundException(msg)
      }
    }
    (asciiDocLogHandler.failIf.severity, asciiDocLogHandler.failIf.containsText) match {
      case (Some(severity), Some(textToSearch)) =>
        val records = memoryLogHandler.filter(severity, textToSearch)
        logRecords(records, s"Found ${records.size} issue(s) matching severity $severity or higher and text '$textToSearch'")
      case (Some(severity), None) =>
        val records = memoryLogHandler.filter(severity)
        logRecords(records, s"Found ${records.size} issue(s) matching severity $severity or higher during rendering")
      case (None, Some(textToSearch)) =>
        val records = memoryLogHandler.filter(textToSearch)
        logRecords(records, s"Found ${records.size} issue(s) containing '$textToSearch'")
      case (None, None) =>
      // LogHandler is disabled
    }
  }

  private def synchronize(asciiDocSynchronizations: List[Synchronization]): Unit =
    asciiDocSynchronizations.foreach(synchronize)

  protected def synchronize(synchronization: Synchronization): Unit = {
    if (synchronization.source.isDirectory)
      try {
        IO.copyDirectory(synchronization.source, synchronization.target, overwrite = true, preserveLastModified = true)
      } catch {
        case _: IOException =>
          logError(String.format("Can't synchronize %s -> %s", synchronization.source, synchronization.target))
      } else
      try {
        IO.copyFile(synchronization.source, synchronization.target, preserveLastModified = true)
      } catch {
        case _: IOException =>
          logError(String.format("Can't synchronize %s -> %s", synchronization.source, synchronization.target))
      }
  }

  protected def logRenderedFile(f: File): Unit = logInfo("Rendered " + f.getAbsolutePath)

  private def setDestinationPaths(
    asciiDocDirectory: File,
    sourceFile: File,
    asciiDocOutputDirectory: File,
    asciiDocOutputFile: Option[File],
    optionsBuilder: OptionsBuilder,
    asciiDocRelativeBaseDir: Boolean,
    asciiDocPreserveDirectories: Boolean
  ): File =
    try {
      // when asciiDocPreserveDirectories == false, parent and asciiDocDirectory are the same
      if (asciiDocRelativeBaseDir) {
        optionsBuilder.baseDir(sourceFile.getParentFile)
      } else {
        optionsBuilder.baseDir(asciiDocDirectory)
      }
      if (asciiDocPreserveDirectories) {
        val candidatePath = sourceFile.getParentFile.getCanonicalPath.substring(asciiDocDirectory.getCanonicalPath.length)
        val relativePath = new File(asciiDocOutputDirectory.getCanonicalPath + candidatePath)
        optionsBuilder.toDir(relativePath).destinationDir(relativePath)
      } else {
        optionsBuilder.toDir(asciiDocOutputDirectory).destinationDir(asciiDocOutputDirectory)
      }

      asciiDocOutputFile match {
        case Some(outFile) =>
          optionsBuilder.toFile(outFile)
          if (outFile.isAbsolute) outFile
          else new File(optionsBuilder.asMap().get(Options.DESTINATION_DIR).asInstanceOf[String], outFile.getPath)
        case None =>
          new File(optionsBuilder.asMap.get(Options.DESTINATION_DIR).asInstanceOf[String], sourceFile.getName)
      }
    } catch {
      case e: IOException =>
        throw new Exception("Unable to locate output directory", e)
    }

  protected def setOptionsOnBuilder(
    asciiDocBackend: String,
    asciiDocHeaderFooter: Boolean,
    asciiDocERuby: String,
    asciiDocSourcemap: Boolean,
    asciiDocCatalogAssets: Boolean,
    asciiDocTemplateCache: Boolean,
    asciiDocType: Option[String],
    asciiDocTemplateEngine: Option[String],
    asciiDocTemplateDir: Option[File]
  ): OptionsBuilder = {
    val optionsBuilder = OptionsBuilder.options
      .backend(asciiDocBackend)
      .safe(SafeMode.UNSAFE)
      .headerFooter(asciiDocHeaderFooter)
      .eruby(asciiDocERuby)
      .mkDirs(true)
    // Following options are only set when the value is different than the default
    if (asciiDocSourcemap) optionsBuilder.option("asciiDocSourcemap", true)
    if (asciiDocCatalogAssets) optionsBuilder.option("catalog_assets", true)
    if (!asciiDocTemplateCache) optionsBuilder.option("template_cache", false)
    if (asciiDocType.isDefined) optionsBuilder.docType(asciiDocType.get)
    if (asciiDocTemplateEngine.isDefined) optionsBuilder.templateEngine(asciiDocTemplateEngine.get)
    if (asciiDocTemplateDir.isDefined) optionsBuilder.templateDir(asciiDocTemplateDir.get)

    optionsBuilder
  }

  protected def setAttributesOnBuilder(
    asciiDocSourceHighlighter: Option[String],
    asciiDocEmbedAssets: Boolean,
    asciiDocImagesDir: Option[String],
    asciiDocAttributeMissing: AttributeMissing,
    asciiDocAttributeUndefined: AttributeUndefined,
    asciiDocAttributes: Map[String, Any],
    asciiDocAttributesChain: String
  ): AttributesBuilder = {
    val attributesBuilder = AttributesBuilder.attributes
    if (asciiDocSourceHighlighter.isDefined) attributesBuilder.sourceHighlighter(asciiDocSourceHighlighter.get)
    if (asciiDocEmbedAssets) {
      attributesBuilder.linkCss(false)
      attributesBuilder.dataUri(true)
    }
    if (asciiDocImagesDir.isDefined) attributesBuilder.imagesDir(asciiDocImagesDir.get)
    attributesBuilder.attributeMissing(asciiDocAttributeMissing.value)
    attributesBuilder.attributeUndefined(asciiDocAttributeUndefined.value)

    AsciiDoctorHelper.addAttributes(asciiDocAttributes, attributesBuilder)
    if (asciiDocAttributesChain.nonEmpty) {
      logInfo("Attributes: " + asciiDocAttributesChain)
      attributesBuilder.arguments(asciiDocAttributesChain)
    }

    attributesBuilder
  }

  protected def getAsciidoctorInstance(gemPathOpt: Option[String]): Asciidoctor = {
    val asciidoctor = Asciidoctor.Factory.create

    val rubyInstance =
      try {
        classOf[JRubyRuntimeContext].getMethod("get").invoke(null).asInstanceOf[Ruby]
      } catch {
        case _: NoSuchMethodException =>
          try {
            classOf[JRubyRuntimeContext].getMethod("get", classOf[Asciidoctor]).invoke(null, asciidoctor).asInstanceOf[Ruby]
          } catch {
            case e1: Exception =>
              logInfo(e1.getMessage)
              throw JRubyRuntimeContextException("Failed to invoke get(AsciiDoctor) for JRubyRuntimeContext", e1)
          }
        case e: Exception =>
          throw JRubyRuntimeContextException("Failed to invoke get for JRubyRuntimeContext", e)
      }

    val gemHome = rubyInstance.evalScriptlet("ENV['GEM_HOME']").toString
    val gemHomeExpected = gemPathOpt match {
      case None => ""
      case Some(path) if path.trim.isEmpty => ""
      case Some(path) => path.split(java.io.File.pathSeparator)(0)
    }

    if (gemHome.nonEmpty && gemHomeExpected != gemHome) {
      logWarn(s"Using inherited external environment to resolve gems ($gemHome), i.e. build is platform dependent!")
    }

    asciidoctor
  }

  private def scanSourceFiles(asciiDocDirectory: File, asciiDocSourceDocumentExtensions: List[String]): List[File] = {
    val absoluteSourceDirectory = asciiDocDirectory.getAbsolutePath
    val asciidoctorFiles =
      if (asciiDocSourceDocumentExtensions.isEmpty) {
        new AsciiDocDirectoryWalker(absoluteSourceDirectory).scan
      } else {
        new CustomExtensionDirectoryWalker(absoluteSourceDirectory, asciiDocSourceDocumentExtensions).scan
      }
    asciidoctorFiles.asScala.filter(f => f.getAbsolutePath != absoluteSourceDirectory && !f.getName.startsWith("_")).toList
  }

}

private class CustomExtensionDirectoryWalker(val absolutePath: String, val fileExtensions: List[String]) extends AbstractDirectoryWalker(absolutePath) {
  override protected def isAcceptedFile(filename: File): Boolean = fileExtensions.exists(filename.getName.endsWith)
}

trait AsciiDoctorPluginKeys {
  val asciiDocConvert: TaskKey[Unit] = taskKey[Unit]("Convert AsciiDoc files to target files")

  val asciiDocAttributeMissing: SettingKey[AttributeMissing] = settingKey[AttributeMissing]("What to do when an attribute is missing")
  val asciiDocAttributeUndefined: SettingKey[AttributeUndefined] = settingKey[AttributeUndefined]("What to do when an attribute is undefined")
  val asciiDocAttributes: SettingKey[Map[String, Any]] = settingKey[Map[String, Any]]("Attributes to pass to Asciidoctor")
  val asciiDocAttributesChain: SettingKey[String] = settingKey[String]("Space separated key=value attributes to pass to Asciidoctor")
  val asciiDocBackend: SettingKey[String] = settingKey[String]("Asciidoctor to use")
  val asciiDocDirectory: SettingKey[File] = settingKey[File]("Directory containing AsciiDoc files")
  val asciiDocCatalogAssets: SettingKey[Boolean] = settingKey[Boolean](
    "Parser to capture images and links in the reference table available via the references property on the document AST object (experimental)"
  )
  val asciiDocType: SettingKey[Option[String]] = settingKey[Option[String]]("Asciidoctor asciiDocType")
  val asciiDocERuby: SettingKey[String] = settingKey[String]("eRuby")
  val asciiDocEmbedAssets: SettingKey[Boolean] = settingKey[Boolean]("Embed the CSS file, etc into the output")
  val asciiDocEnableVerbose: SettingKey[Boolean] = settingKey[Boolean]("verbose")
  val asciiDocEncoding: SettingKey[String] = settingKey[String]("Encoding of Asciidoctor source documents")
  val asciiDocExtensionPattern: SettingKey[Regex] = settingKey[Regex]("Regex for AsciiDoc file extension")
  val asciiDocExtensions: SettingKey[List[ExtensionConfiguration]] =
    settingKey[List[ExtensionConfiguration]]("List of extensions to include during the conversion process")
  val asciiDocGemPath: SettingKey[Option[String]] =
    settingKey[Option[String]]("Location to one or more gem installation directories (same as GEM_PATH environment var)")
  val asciiDocHeaderFooter: SettingKey[Boolean] = settingKey[Boolean]("Enable header and footer")
  val asciiDocImagesDir: SettingKey[Option[String]] = settingKey[Option[String]]("Path to directory containing images, relative to source directory")
  val asciiDocLogHandler = settingKey[LogHandler]("enables processing of Asciidoctor messages")
  val asciiDocOutputDirectory: SettingKey[File] = settingKey[File]("Default directory target directory to containing converted files")
  val asciiDocOutputFile: SettingKey[Option[File]] = settingKey[Option[File]]("Used to override the name of the generated output file")
  val asciiDocPreserveDirectories: SettingKey[Boolean] =
    settingKey[Boolean]("Whether the documents should be rendered in the same folder structure as in the source directory or not")
  val asciiDocRelativeBaseDir: SettingKey[Boolean] = settingKey[Boolean]("Search in the same folder as AsciiDoc file, only used when baseDir not set")
  val asciiDocRequires: SettingKey[List[String]] = settingKey[List[String]]("Additional Ruby libraries not packaged in AsciidoctorJ")
  val asciiDocResources: SettingKey[List[File]] = settingKey[List[File]]("List of resources to copy to the output directory (e.g., images, css)")
  val asciiDocSourceDocumentExtensions: SettingKey[List[String]] =
    settingKey[List[String]]("(named extensions in v1.5.3 and below) a List<String> of non-standard file extensions to render")
  val asciiDocSourceDocumentName: SettingKey[Option[String]] = settingKey[Option[String]]("An override to process a single source file")
  val asciiDocSourceHighlighter: SettingKey[Option[String]] = settingKey[Option[String]]("Enable and set the source highlighter")
  val asciiDocSourcemap: SettingKey[Boolean] =
    settingKey[Boolean]("Adds file and line number information to each parsed block (lineno and source_location attributes)")
  val asciiDocSynchronizations: SettingKey[List[Synchronization]] = settingKey[List[Synchronization]]("Files to synchronize to target")
  val asciiDocTemplateCache: SettingKey[Boolean] =
    settingKey[Boolean]("Enable the built-in cache used by the template converter when reading the source of template files")
  val asciiDocTemplateDir: SettingKey[Option[sbt.File]] =
    settingKey[Option[File]]("Directory of Tilt-compatible templates to be used instead of the default built-in templates")
  val asciiDocTemplateEngine: SettingKey[Option[String]] = settingKey[Option[String]]("Template engine to use for the custom converter templates")
  val asciiDocTitle: SettingKey[String] = settingKey[String]("An override for the title of the document")
}
