name := "GitReport"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.1"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.0.201612231935-r"

libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0"

libraryDependencies += "com.github.jknack" % "handlebars" % "4.0.6"

libraryDependencies += "com.typesafe" % "config" % "1.3.1"

assemblyJarName in assembly := "git-report.jar"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.2" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test"

testOptions in Test += Tests.Argument("-oI")
