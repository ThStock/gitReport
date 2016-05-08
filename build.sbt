name := "GitReport"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.3.1.201605051710-r"

libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.4"

libraryDependencies += "com.gilt" %% "handlebars-scala" % "2.1.1"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

assemblyJarName in assembly := "git-report.jar"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"

testOptions in Test += Tests.Argument("-oI")

// java8 with scala 2.11.7 down this line
libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0"

scalacOptions ++= List("-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8", "-Yopt:l:classpath")
