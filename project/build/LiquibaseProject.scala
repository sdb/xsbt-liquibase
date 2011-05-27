import sbt._

class LiquibaseProject(info: ProjectInfo) extends PluginProject(info) {
  lazy val snapshot = projectVersion.value.toString.endsWith("SNAPSHOT")

  override def managedStyle = ManagedStyle.Maven
  lazy val publishTo = Resolver.file("GitHub Pages",
    new java.io.File("../sdb.github.com/maven", if (snapshot) "snapshots" else "releases"))

  val liquibase = "org.liquibase" % "liquibase-core" % "2.0-rc6"
}
