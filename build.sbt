name := "GitReport"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "3.6.1.201501031845-r"

libraryDependencies += "com.gilt" %% "handlebars-scala" % "2.0.1"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

assemblyJarName in assembly := "git-report.jar"
