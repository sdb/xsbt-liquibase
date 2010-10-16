package com.github.sdb.sbt.liquibase

import _root_.sbt._
import Configurations.Runtime

import _root_.liquibase.integration.commandline.CommandLineUtils
import _root_.liquibase.resource.FileSystemResourceAccessor
import _root_.liquibase.Liquibase
import _root_.liquibase.servicelocator.ServiceLocator
import _root_.liquibase.exception._


trait LiquibasePlugin extends Project with ClasspathProject {

  def liquibaseChangeLogFile: Path
  def liquibaseUrl: String
  def liquibaseDriver: String

  def liquibaseUsername: String = null
  def liquibasePassword: String = null
  
  def liquibaseContexts: String = null
  def liquibaseDefaultSchemaName: String = null
  

  // pass the project logger to receive logging from Liquibase
  SBTLogger.logger = Some(log)


  def liquibaseDatabase = CommandLineUtils.createDatabaseObject(
    ClasspathUtilities.toLoader(fullClasspath(Runtime)),
    liquibaseUrl,
    liquibaseUsername,
    liquibasePassword,
    liquibaseDriver,
    liquibaseDefaultSchemaName,
    null)

  def liquibase = new Liquibase(
    liquibaseChangeLogFile.absolutePath,
    new FileSystemResourceAccessor,
    liquibaseDatabase)


  private implicit def action2Result(a: LiquibaseAction) = a.run

  object Int {
    def unapply(s: String): Option[Int] = try {
      Some(s.toInt)
    } catch {
      case _ : java.lang.NumberFormatException => None
    }
  }

  lazy val liquibaseUpdate = liquibaseUpdateAction
  def liquibaseUpdateAction = task {
    new LiquibaseAction({lb => lb update liquibaseContexts; None }) with Cleanup
  } describedAs  "Applies un-run changes to the database."


  lazy val liquibaseUpdateCount = liquibaseUpdateCountAction
  def liquibaseUpdateCountAction = taskWithArgs { args => {
    new LiquibaseAction({ lb =>
      args.size match {
        case 1 => args(0) match {
          case Int(x) => lb update(x, liquibaseContexts); None
          case _ => Some("Number of change sets must be an integer value.")
        }
        case _ => Some("Number of change sets must be specified.")
      }
    }) with Cleanup }
  } describedAs  "Applies the next number of change sets."


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
        case 1 => lb rollback(args(0), liquibaseContexts); None
        case _ => Some("The tag must be specified.")
      }
    }) with Cleanup }
  } describedAs  "Rolls back the database to the state it was in when the tag was applied."


  lazy val liquibaseClearChecksums = liquibaseClearChecksumsAction
  def liquibaseClearChecksumsAction = task {
    new LiquibaseAction({ lb => lb clearCheckSums; None }) with Cleanup
  } describedAs  "Removes current checksums from database."


  lazy val liquibaseValidate = liquibaseValidateAction
  def liquibaseValidateAction = task {
    new LiquibaseAction({ lb => lb validate; None }) with Cleanup
  } describedAs  "Checks the changelog for errors."

    
  def taskWithArgs(t: (Array[String]) => Option[String]) =
    task { args => task { t(args) } }


  abstract class LiquibaseAction(action: Liquibase => Option[String]) {
    lazy val liquibase = LiquibasePlugin.this.liquibase
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


import _root_.liquibase.logging.core.AbstractLogger

object SBTLogger {

  // make sure that our custom Liquibase logger can be discovered by the
  // service locator
  ServiceLocator.getInstance.addPackageToScan("com.github.sdb.sbt.liquibase")

  var logger: Option[Logger] = None

  def log(l: String, m: String, e: Throwable) {
    logger match {
      case Some(log) => {
        l match {
          case "debug" => log.debug(m)
          case "info"  => log.info(m)
          case "warn"  => log.warn(m)
          case "error" => log.error(m)
        }
        if (e != null) log.trace(e)
      }
      case None => {}
    }
  }
}

/**
 * Custom Liquibase logger to redirect logging to the project logger.
 */
class SBTLogger extends AbstractLogger {
  import SBTLogger._
  def getPriority = 10
  def setLogLevel(l: String, f: String) {} // ignored 
  def setName(n: String) {} // ignored 

  def debug(m: String, e: Throwable) { log("debug", m, e) }
  def debug(m: String) { log("debug", m, null) }
  def info(m: String, e: Throwable) { log("info", m, e) }
  def info(m: String) { log("info", m, null) }
  def warning(m: String, e: Throwable) { log("warn", m, e) }
  def warning(m: String) { log("warn", m, null) }
  def severe(m: String, e: Throwable) { log("error", m, e) }
  def severe(m: String) { log("error", m, null) }
}

