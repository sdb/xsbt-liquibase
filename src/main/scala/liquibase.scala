package com.github.sdb.sbt.liquibase

import _root_.sbt._
import Configurations.Runtime

import _root_.liquibase.integration.commandline.CommandLineUtils
import _root_.liquibase.resource.FileSystemResourceAccessor
import _root_.liquibase.Liquibase
import _root_.liquibase.exception._


trait LiquiBasePlugin extends Project with ClasspathProject {

  def changeLogFile: Path
  def url: String
  def driver: String
  
  def contexts: String = null
  def defaultSchemaName: String = null

  lazy val liquibaseUpdate = liquibaseUpdateAction
  def liquibaseUpdateAction = task {
    new LiquibaseAction with Cleanup {
      def action { liquibase update contexts }
    }.run
    None
  } describedAs  "Applies un-run changes to the database."


  trait LiquibaseAction {

    lazy val database = CommandLineUtils.createDatabaseObject(
      ClasspathUtilities.toLoader(fullClasspath(Runtime)),
      url,
      null,
      null,
      driver,
      defaultSchemaName,
      null)

    lazy val liquibase = new Liquibase(
      changeLogFile.absolutePath,
      new FileSystemResourceAccessor,
      database)

    def action
    def run = exec({ () => action})
    def exec(f: () => Unit) = f()
  }

  trait Cleanup extends LiquibaseAction {

    override def exec(f: () => Unit) {
      try {
        f()
      } finally {
        cleanup
      }
    }

    def cleanup {
      if (liquibase != null)
        try {
          liquibase.forceReleaseLocks
        } catch {
          case e: LiquibaseException => log trace e
        }
      if (database != null)
        try {
          database.rollback
          database.close
        } catch {
          case e: DatabaseException => log trace e
        }
    }
  }

}

