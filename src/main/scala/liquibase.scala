package com.github.sdb.sbt.liquibase

import _root_.sbt._
import Configurations.Runtime

import _root_.liquibase.integration.commandline.CommandLineUtils
import _root_.liquibase.resource.FileSystemResourceAccessor
import _root_.liquibase.Liquibase
import _root_.liquibase.database.Database
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

  lazy val liquibaseDrop = liquibaseDropAction
  def liquibaseDropAction = task { args => task {
    new LiquibaseAction with Cleanup {
      def action {
        val schemas = args.toList.toArray
        if (schemas.size > 0) liquibase dropAll (schemas:_*)
        else liquibase dropAll
      }
    }.run
    None }
  } describedAs  "Drops database objects owned by the current user."

  def database = CommandLineUtils.createDatabaseObject(
    ClasspathUtilities.toLoader(fullClasspath(Runtime)),
    url,
    null,
    null,
    driver,
    defaultSchemaName,
    null)

  def liquibase = new Liquibase(
    changeLogFile.absolutePath,
    new FileSystemResourceAccessor,
    database)


  trait LiquibaseAction {
    lazy val liquibase = LiquiBasePlugin.this.liquibase
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
      val db = liquibase.getDatabase
      if (db != null)
        try {
          db.rollback
          db.close
        } catch {
          case e: DatabaseException => log trace e
        }
    }
  }

}

