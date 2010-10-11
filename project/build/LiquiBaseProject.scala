import sbt._

class LiquiBaseProject(info: ProjectInfo) extends PluginProject(info) {
  val liquiBase = "org.liquibase" % "liquibase-core" % "2.0-rc6"
}
