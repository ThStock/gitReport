name := "GitReport"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "3.6.1.201501031845-r"

libraryDependencies += "com.gilt" %% "handlebars-scala" % "2.0.1"

assemblyJarName in assembly := "git-report.jar"