name := "GitReport"

version := "1.0"

scalaVersion := "2.11.10"

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.10"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.6.0.201612231935-r"

libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.4"

libraryDependencies += "com.github.jknack" % "handlebars" % "4.0.6"

libraryDependencies += "com.typesafe" % "config" % "1.3.1"

assemblyJarName in assembly := "git-report.jar"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"

testOptions in Test += Tests.Argument("-oI")

// java8 with scala 2.11.7 down this line
libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0"

scalacOptions ++= List("-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8", "-Yopt:l:classpath")
