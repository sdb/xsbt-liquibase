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

  def username: String = null
  def password: String = null
  
  def contexts: String = null
  def defaultSchemaName: String = null


  def database = CommandLineUtils.createDatabaseObject(
    ClasspathUtilities.toLoader(fullClasspath(Runtime)),
    url,
    username,
    password,
    driver,
    defaultSchemaName,
    null)

  def liquibase = new Liquibase(
    changeLogFile.absolutePath,
    new FileSystemResourceAccessor,
    database)


  private implicit def action2Result(a: LiquibaseAction) = a.run

  lazy val liquibaseUpdate = liquibaseUpdateAction
  def liquibaseUpdateAction = task {
    new LiquibaseAction({lb => lb update contexts; None }) with Cleanup
  } describedAs  "Applies un-run changes to the database."


  lazy val liquibaseDrop = liquibaseDropAction
  def liquibaseDropAction = taskWithArgs { args => {
    new LiquibaseAction({ lb =>
      args.size match {
        case 0 => lb dropAll
        case _ => lb dropAll (args:_*)
      }; None
    }) with Cleanup }
  } describedAs  "Drops database objects owned by the current user."


  lazy val liquibaseTag = liquibaseTagAction
  def liquibaseTagAction = taskWithArgs { args => {
    new LiquibaseAction({ lb =>
      args.size match {
        case 1 => lb tag args(0); None
        case _ => Some("The tag must be specified.")
      }
    }) with Cleanup }
  } describedAs  "Tags the current database state for future rollback."


  lazy val liquibaseRollback = liquibaseRollbackAction
  def liquibaseRollbackAction = taskWithArgs { args => {
    new LiquibaseAction({ lb =>
      args.size match {
        case 1 => lb rollback(args(0), contexts); None
        case _ => Some("The tag must be specified.")
      }
    }) with Cleanup }
  } describedAs  "Rolls back the database to the state it was in when the tag was applied."

    
  def taskWithArgs(t: (Array[String]) => Option[String]) =
    task { args => task { t(args) } }


  abstract class LiquibaseAction(action: Liquibase => Option[String]) {
    lazy val liquibase = LiquiBasePlugin.this.liquibase
    def run: Option[String] = exec({ action(liquibase) })
    def exec(f: => Option[String]) = f
  }

  trait Cleanup extends LiquibaseAction {

    override def exec(f: => Option[String]): Option[String] = {
      try { return f } finally { cleanup }
    }

    def cleanup {
      if (liquibase != null)
        try { liquibase.forceReleaseLocks } catch {
          case e: LiquibaseException => log trace e
        }
      val db = liquibase.getDatabase
      if (db != null)
        try { db.rollback; db.close } catch {
          case e: DatabaseException => log trace e
        }
    }
  }

}

