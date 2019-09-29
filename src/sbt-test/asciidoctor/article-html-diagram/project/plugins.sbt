libraryDependencies += "org.jruby"       % "jruby-complete"       % "9.2.8.0"
libraryDependencies += "org.yaml"        % "snakeyaml"            % "1.25"
libraryDependencies += "org.asciidoctor" % "asciidoctorj"         % "2.1.0"
libraryDependencies += "org.asciidoctor" % "asciidoctorj-pdf"     % "1.5.0-alpha.18"
libraryDependencies += "org.asciidoctor" % "asciidoctorj-diagram" % "1.5.18"

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.mdsol" % "sbt-asciidoctor" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}
