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

2. Mixin the LiquiBasePlugin trait in your project file, e.g. `project/build/TestProject.scala`

        import sbt._
        import com.github.sdb.sbt.liquibase.LiquiBasePlugin

        class TestProject(info: ProjectInfo) extends DefaultProject(info)
          with LiquiBasePlugin {
          ...
        }

3. Configure

        class TestProject(info: ProjectInfo) extends DefaultProject(info)
          with LiquiBasePlugin {

          // declare the required database driver as a runtime dependency
          val h2 = "com.h2database" % "h2" % "1.2.143" % "runtime"

          // provide the parameters for running liquibase commands
          lazy val changeLogFile = "config" / "db-changelog.xml"
          lazy val driver = "org.h2.Driver"
          lazy val url = "jdbc:h2:mem:"
          
          // provide username and password for database access
          override lazy val username = "sa"
          override lazy val password = ""
        }

  Note that this a very basic way to configure the plugin. Take a look at this [gist](http://gist.github.com/624275) for a more realistic example.

#Usage#

The following actions are available:

* `liquibase-update`

  Applies un-run changes to the database.

* `liquibase-drop [SCHEMA]...`

  Drops database objects owned by the current user.

* `liquibase-tag TAG`

  Tags the current database state for future rollback.

* `liquibase-rollback TAG`

  Rolls back the database to the state it was in when the tag was applied.

#Questions and Feedback#

Feel free to send me a message [@sdeboey](http://twitter.com/sdeboey) or check out my [Contact](http://stefandeboey.be/contact) page for other ways to get in touch.
