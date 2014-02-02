import sbt._
import Keys._

object ProjectBuild extends Build {

  override lazy val settings = super.settings ++
  Seq(scalaVersion := "2.10.3", resolvers := Seq())

  val appDependencies = Seq(
    "org.eclipse.jgit" % "org.eclipse.jgit" % "3.2.0.201312181205-r"
    )
  val appResolvers = Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "jGit" at "http://download.eclipse.org/jgit/maven"
    )

  lazy val root = Project(
    id = "GitReport",
    base = file("."),
    settings = Project.defaultSettings ++
    Seq(libraryDependencies ++= appDependencies,
      resolvers ++= appResolvers)
    )
}
