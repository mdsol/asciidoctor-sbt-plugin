package com.asciidoctor.sbt

import java.io.{File, IOException}
import java.util.logging.Logger

import com.asciidoctor.sbt.extensions.{AsciidoctorJExtensionRegistry, ExtensionConfiguration}
import com.asciidoctor.sbt.log.{LogHandler, LogRecordHelper, MemoryLogHandler}
import org.asciidoctor._
import org.asciidoctor.log.LogRecord
import org.jruby.Ruby
import sbt.Keys._
import sbt.{Def, _}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.matching.Regex

object AsciiDoctorPlugin extends AutoPlugin with PluginLogger {

  object autoImport extends AsciiDoctorPluginKeys
  import autoImport._

  lazy val asciiDoctorSettings: Seq[Setting[_]] = Seq(
    asciiDocExtensionPattern := raw""".*\\.a((sc(iidoc)?)|d(oc)?)\$$""".r,
    encoding := "",
    sourceDirectory := baseDirectory.value / "src" / "main" / "asciidoc",
    outputDirectory := target.value / "generated-docs",
    outputFile := None,
    preserveDirectories := false,
    relativeBaseDir := false,
    baseDir := sourceDirectory.value,
    gemPath := Some(""),
    asciiDocRequires := List.empty,
    attributes := Map.empty,
    attributesChain := "",
    backend := "docbook",
    doctype := None,
    eruby := "",
    headerFooter := true,
    templateDir := None,
    templateEngine := None,
    templateCache := true,
    imagesDir := Some("images@"), // '@' Allows override by :imagesdir: document attribute
    sourceHighlighter := None,
    title := "",
    sourceDocumentName := None,
    sourceDocumentExtensions := List.empty,
    sourcemap := false,
    catalogAssets := false,
    synchronizations := List.empty,
    asciiDocExtensions := List.empty,
    embedAssets := false,
    attributeMissing := AttributeMissing.Skip,
    attributeUndefined := AttributeUndefined.DropLine,
    resources := List.empty,
    enableVerbose := false,
    convert := convertDocsTask.value
  )
  override lazy val projectSettings: Seq[Setting[_]] = inConfig(AsciiDoctor)(asciiDoctorSettings)

  private def convertDocsTask: Def.Initialize[Task[AsciiDocResult]] = Def.task {
    val skp = (skip in publish).value
    val ref = thisProjectRef.value
    if (skp) {
      logDebug(s"Skipping AsciiDoc convert for ${ref.project}")
      Skipped
    }
    if (!sourceDirectory.value.exists) {
      logInfo(s"sourceDirectory ${sourceDirectory.value.getPath} does not exist. Skip processing")
      Skipped
    }

    ensureOutputExists()

    if (resources.value.nonEmpty) {
      resources.value.foreach { resource =>
        if (!resource.exists || (resource.isDirectory && resource.list.length == 0)) {
          throw AsciiDoctorEmptyResourcesException
        }
      }
    }

    val asciidoctor = getAsciidoctorInstance(gemPath.value)

    if (enableVerbose.value) asciidoctor.requireLibrary("enable_verbose.rb")

    asciidoctor.requireLibraries(asciiDocRequires.value.asJava)

    val optionsBuilder = OptionsBuilder.options
    setOptionsOnBuilder(optionsBuilder)

    val attributesBuilder = AttributesBuilder.attributes
    setAttributesOnBuilder(attributesBuilder)

    optionsBuilder.attributes(attributesBuilder)

    val extensionRegistry = new AsciidoctorJExtensionRegistry(asciidoctor)
    asciiDocExtensions.value.foreach { extension =>
      extensionRegistry.register(extension.className, extension.blockName)
    }

    // TODO: implement copyResources

    val sourceFiles: Seq[File] = sourceDocumentName.value match {
      case Some(srcDocName) => List(new File(sourceDirectory.value, srcDocName))
      case None             => scanSourceFiles
    }

    // register LogHandler to capture asciidoctor messages
    val memoryLogHandler = new MemoryLogHandler(logHandler.value.outputToConsole, sourceDirectory.value)
    if (sourceFiles.nonEmpty) {
      asciidoctor.registerLogHandler(memoryLogHandler)
      // disable default console output of AsciidoctorJ
      Logger.getLogger("asciidoctor").setUseParentHandlers(false)
    }

    renderFiles(memoryLogHandler, asciidoctor, optionsBuilder, sourceFiles, Set.empty)

    if (synchronizations.value.nonEmpty) synchronize()

    Success
  }

  @tailrec
  private def renderFiles(memoryLogHandler: MemoryLogHandler,
                          asciidoctor: Asciidoctor,
                          optionsBuilder: OptionsBuilder,
                          sourceFiles: Seq[File],
                          renderedFiles: Set[File]): Set[File] = {
    sourceFiles match {
      case Nil => renderedFiles
      case source :: tail =>
        val destinationPath = setDestinationPaths(optionsBuilder, source)
        val updatedRenderedFiles = renderedFiles + destinationPath

        if (renderedFiles.size == updatedRenderedFiles.size) {
          logWarn(s"Duplicated destination found: overwriting file: ${destinationPath.getAbsolutePath}")
        }

        renderFile(asciidoctor, optionsBuilder, source)
        processLogMessages(memoryLogHandler)
        renderFiles(memoryLogHandler, asciidoctor, optionsBuilder, tail, updatedRenderedFiles)
    }
  }

  protected def renderFile(asciidoctor: Asciidoctor, options: OptionsBuilder, f: File): Unit = {
    asciidoctor.convertFile(f, options)
    logRenderedFile(f)
  }

  private def processLogMessages(memoryLogHandler: MemoryLogHandler): Unit = {
    def logRecords(records: List[LogRecord], msg: String): Unit = {
      records.foreach { record =>
        logError(LogRecordHelper.format(record, sourceDirectory.value))
      }
      if (records.nonEmpty) {
        throw AsciiDoctorIssuesFoundException(msg)
      }
    }
    val lgHandler = logHandler.value
    (lgHandler.failIf.severity, lgHandler.failIf.containsText) match {
      case (Some(severity), Some(textToSearch)) =>
        val records = memoryLogHandler.filter(severity, textToSearch)
        logRecords(records, s"Found ${records.size} issue(s) matching severity $severity or higher and text '$textToSearch'")
      case (Some(severity), None) =>
        val records = memoryLogHandler.filter(severity)
        logRecords(records, s"Found ${records.size} issue(s) matching severity $severity or higher during rendering")
      case (None, Some(textToSearch)) =>
        val records = memoryLogHandler.filter(textToSearch)
        logRecords(records, s"Found ${records.size} issue(s) containing '$textToSearch'")
    }
  }

  private def synchronize(): Unit = {
    synchronizations.value.foreach(synchronize)
  }

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
        case e: IOException =>
          logError(String.format("Can't synchronize %s -> %s", synchronization.source, synchronization.target))
      }
  }

  protected def logRenderedFile(f: File): Unit = logInfo("Rendered " + f.getAbsolutePath)

  protected def ensureOutputExists(): Unit = {
    if (!outputDirectory.value.exists && !outputDirectory.value.mkdirs) logError("Can't create " + outputDirectory.value.getPath)
  }

  private def setDestinationPaths(optionsBuilder: OptionsBuilder, sourceFile: File): File =
    try {
      if (baseDir.value != null) {
        optionsBuilder.baseDir(baseDir.value)
      } else { // when preserveDirectories == false, parent and sourceDirectory are the same
        if (relativeBaseDir.value) {
          optionsBuilder.baseDir(sourceFile.getParentFile)
        } else {
          optionsBuilder.baseDir(sourceDirectory.value)
        }
      }
      if (preserveDirectories.value) {
        val candidatePath = sourceFile.getParentFile.getCanonicalPath.substring(sourceDirectory.value.getCanonicalPath.length)
        val relativePath = new File(outputDirectory.value.getCanonicalPath + candidatePath)
        optionsBuilder.toDir(relativePath).destinationDir(relativePath)
      } else {
        optionsBuilder.toDir(outputDirectory.value).destinationDir(outputDirectory.value)
      }

      outputFile.value match {
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

  protected def setOptionsOnBuilder(optionsBuilder: OptionsBuilder): Unit = {
    optionsBuilder
      .backend(backend.value)
      .safe(SafeMode.UNSAFE)
      .headerFooter(headerFooter.value)
      .eruby(eruby.value)
      .mkDirs(true)
    // Following options are only set when the value is different than the default
    if (sourcemap.value) optionsBuilder.option("sourcemap", true)
    if (catalogAssets.value) optionsBuilder.option("catalog_assets", true)
    if (!templateCache.value) optionsBuilder.option("template_cache", false)
    if (doctype.value.isDefined) optionsBuilder.docType(doctype.value.get)
    if (templateEngine.value.isDefined) optionsBuilder.templateEngine(templateEngine.value.get)
    if (templateDir.value.isDefined) optionsBuilder.templateDir(templateDir.value.get)
  }

  protected def setAttributesOnBuilder(attributesBuilder: AttributesBuilder): Unit = {
    if (sourceHighlighter.value.isDefined) attributesBuilder.sourceHighlighter(sourceHighlighter.value.get)
    if (embedAssets.value) {
      attributesBuilder.linkCss(false)
      attributesBuilder.dataUri(true)
    }
    if (imagesDir.value.isDefined) attributesBuilder.imagesDir(imagesDir.value.get)
    attributesBuilder.attributeMissing(attributeMissing.value.value)
    attributesBuilder.attributeUndefined(attributeUndefined.value.value)

    AsciiDoctorHelper.addAttributes(attributes.value, attributesBuilder)
    if (attributesChain.value.nonEmpty) {
      logInfo("Attributes: " + attributesChain)
      attributesBuilder.arguments(attributesChain.value)
    }
  }

  protected def getAsciidoctorInstance(gemPathOpt: Option[String]): Asciidoctor = {
    val asciidoctor = gemPathOpt match {
      case Some(path) if File.separatorChar == '\\' => Asciidoctor.Factory.create(path.replaceAll("\\\\", "/"))
      case Some(path)                               => Asciidoctor.Factory.create(path)
      case None                                     => Asciidoctor.Factory.create
    }

    val rubyInstance =
      try {
        classOf[JRubyRuntimeContext].getMethod("get").invoke(null).asInstanceOf[Ruby]
      } catch {
        case _: NoSuchMethodException =>
          try {
            classOf[JRubyRuntimeContext].getMethod("get", classOf[Asciidoctor]).invoke(null, asciidoctor).asInstanceOf[Ruby]
          } catch {
            case e1: Exception =>
              throw JRubyRuntimeContext("Failed to invoke get(AsciiDoctor) for JRubyRuntimeContext", e1)
          }
        case e: Exception =>
          throw JRubyRuntimeContext("Failed to invoke get for JRubyRuntimeContext", e)
      }

    val gemHome = rubyInstance.evalScriptlet("ENV['GEM_HOME']").toString
    val gemHomeExpected = gemPathOpt match {
      case None | Some(path) if path.trim.isEmpty => ""
      case Some(path)                             => path.split(java.io.File.pathSeparator)(0)
    }

    if (gemHome.nonEmpty && gemHomeExpected != gemHome) {
      logWarn(s"Using inherited external environment to resolve gems ($gemHome), i.e. build is platform dependent!")
    }

    asciidoctor
  }

  private def scanSourceFiles: List[File] = {
    val absoluteSourceDirectory = sourceDirectory.value.getAbsolutePath
    val srcDocExts = sourceDocumentExtensions.value
    val asciidoctorFiles =
      if (srcDocExts.isEmpty) {
        new AsciiDocDirectoryWalker(absoluteSourceDirectory).scan
      } else {
        new CustomExtensionDirectoryWalker(absoluteSourceDirectory, srcDocExts).scan
      }
    asciidoctorFiles.asScala.filter(f => f.getAbsolutePath != absoluteSourceDirectory && !f.getName.startsWith("_")).toList
  }

}

private class CustomExtensionDirectoryWalker(val absolutePath: String, val fileExtensions: List[String]) extends AbstractDirectoryWalker(absolutePath) {
  override protected def isAcceptedFile(filename: File): Boolean = fileExtensions.exists(filename.getName.endsWith)
}

trait AsciiDoctorPluginKeys {
  val AsciiDoctor: Configuration = config("asciidoctor") extend Compile

  val convert: TaskKey[Unit] = taskKey[Unit]("Convert AsciiDoc files to target files")

  val asciiDocExtensionPattern: SettingKey[Regex] = settingKey[Regex]("Regex for AsciiDoc file extension")

  val encoding: SettingKey[String] = settingKey[String]("Encoding of Asciidoctor source documents")

  val sourceDirectory: SettingKey[File] = settingKey[File]("Default directory containing Asciidoctor sources.")

  val outputDirectory: SettingKey[File] = settingKey[File]("Default directory target directory to containing converted files")

  val outputFile: SettingKey[Option[sbt.File]] = settingKey[Option[File]]("Used to override the name of the generated output file")

  val preserveDirectories: SettingKey[Boolean] =
    settingKey[Boolean]("Whether the documents should be rendered in the same folder structure as in the source directory or not")

  val relativeBaseDir: SettingKey[Boolean] = settingKey[Boolean]("Search in the same folder as AsciiDoc file, only used when baseDir not set")

  val baseDir: SettingKey[File] = settingKey[File]("Root path for resources")

  val gemPath: SettingKey[Option[String]] =
    settingKey[Option[String]]("Location to one or more gem installation directories (same as GEM_PATH environment var)")

  val asciiDocRequires: SettingKey[List[String]] = settingKey[List[String]]("Additional Ruby libraries not packaged in AsciidoctorJ")

  val attributes: SettingKey[Map[String, Any]] = settingKey[Map[String, Any]]("Attributes to pass to Asciidoctor")

  val attributesChain: SettingKey[String] = settingKey[String]("Space separated key=value attributes to pass to Asciidoctor")

  val backend: SettingKey[String] = settingKey[String]("Asciidoctor to use")

  val doctype: SettingKey[Option[String]] = settingKey[Option[String]]("Asciidoctor doctype")

  val eruby: SettingKey[String] = settingKey[String]("eRuby")

  val headerFooter: SettingKey[Boolean] = settingKey[Boolean]("Enable header and footer")

  val templateDir: SettingKey[Option[sbt.File]] =
    settingKey[Option[File]]("Directory of Tilt-compatible templates to be used instead of the default built-in templates")

  val templateEngine: SettingKey[Option[String]] = settingKey[Option[String]]("Template engine to use for the custom converter templates")

  val templateCache: SettingKey[Boolean] =
    settingKey[Boolean]("Enable the built-in cache used by the template converter when reading the source of template files")

  val imagesDir: SettingKey[Option[String]] = settingKey[Option[String]]("Path to directory containing images, relative to source directory")

  val sourceHighlighter: SettingKey[Option[String]] = settingKey[Option[String]]("Enable and set the source highlighter")

  val title: SettingKey[String] = settingKey[String]("An override for the title of the document")

  val sourceDocumentName: SettingKey[Option[String]] = settingKey[Option[String]]("An override to process a single source file")

  val sourceDocumentExtensions: SettingKey[List[String]] =
    settingKey[List[String]]("(named extensions in v1.5.3 and below) a List<String> of non-standard file extensions to render")
  val sourcemap: SettingKey[Boolean] = settingKey[Boolean]("Adds file and line number information to each parsed block (lineno and source_location attributes)")

  val catalogAssets: SettingKey[Boolean] =
    settingKey[Boolean](
      "Parser to capture images and links in the reference table available via the references property on the document AST object (experimental)"
    )

  val synchronizations: SettingKey[List[Synchronization]] = settingKey[List[Synchronization]]("Files to synchronize to target")

  val asciiDocExtensions: SettingKey[List[ExtensionConfiguration]] =
    settingKey[List[ExtensionConfiguration]]("List of extensions to include during the conversion process")

  val embedAssets: SettingKey[Boolean] = settingKey[Boolean]("Embed the CSS file, etc into the output")

  val attributeMissing: SettingKey[AttributeMissing] = settingKey[AttributeMissing]("What to do when an attribute is missing")

  val attributeUndefined: SettingKey[AttributeUndefined] = settingKey[AttributeUndefined]("What to do when an attribute is undefined")

  val resources: SettingKey[List[File]] = settingKey[List[File]]("List of resources to copy to the output directory (e.g., images, css)")

  val enableVerbose: SettingKey[Boolean] = settingKey[Boolean]("verbose")

  val logHandler = settingKey[LogHandler]("enables processing of Asciidoctor messages")
}
