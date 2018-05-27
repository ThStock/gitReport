name := "GitReport"

version := "1.0"

scalaVersion := "2.12.6"

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.6"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.11.0.201803080745-r"

libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0"

libraryDependencies += "com.github.jknack" % "handlebars" % "4.0.6"

libraryDependencies += "com.typesafe" % "config" % "1.3.3"

assemblyJarName in assembly := "git-report.jar"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test"

scalacOptions := Seq("-unchecked", "-deprecation")

testOptions in Test += Tests.Argument("-oI")
