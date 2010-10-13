import sbt._

class LiquibaseProject(info: ProjectInfo) extends PluginProject(info) {
  val liquibase = "org.liquibase" % "liquibase-core" % "2.0-rc6"
}
