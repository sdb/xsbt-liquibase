The liquibase-sbt-plugin is a plugin for the [Simple Build Tool](http://code.google.com/p/simple-build-tool/) (SBT) for running LiquiBase commands.

[Liquibase](http://www.liquibase.org/) is a database-independent library for tracking, managing and applying database changes.

#Install#

The liquibase-sbt-plugin is not (yet) available in a public repository, so you have to build and install it yourself.

    git clone git://github.com/sdb/liquibase-sbt-plugin.git
    cd liquibase-sbt-plugin
    sbt publish-local
    

#Setup#

1. Define a dependency on the liquibase-sbt-plugin in your plugin definition file, `project/plugins/Plugins.scala`

        import sbt._

        class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
          val liquiBase = "com.github.sdb" % "liquibase-sbt-plugin" % "0.0.1"
        }

2. Mixin the LiquiBasePlugin trait in your project file

        import sbt._
        import com.github.sdb.sbt.liquibase.LiquiBasePlugin

        class TestProject(info: ProjectInfo) extends DefaultProject(info) with LiquiBasePlugin {
          ...
        }
