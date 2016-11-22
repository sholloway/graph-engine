package org.machine.engine

import com.typesafe.config._
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import org.machine.engine.communication.WebServer
import org.machine.engine.exceptions._

// import akka.stream.FlowMaterializer
// import akka.stream.scaladsl.PublisherSource

/*
Challenges
Configuration that needs to live outside of the Jar.
- User Settings
- Where the database should be. Right now it's target/Engine.graph This doesn't make sense.
*/
object Main extends LazyLogging{
  import org.machine.communication.harness.STDEngineHarness._

  private implicit var engine:Option[Engine] = None
  private implicit var server:Option[WebServer] = None

  def main(args: Array[String]):Unit = {
    logger.info("Starting up the Engine.")
    try{
      logConfiguration
      initializeWebServer
      initializeGraphEngine
      startWebServer
      startStdinHarness
    }catch{
      case e: Throwable => {
        logger.error("An exception was thrown during startup.", e)
        System.exit(1)
      }
    }
  }

  private def initializeWebServer = {
    server = Some(new WebServer())
  }

  private def initializeGraphEngine = {
    engine = Some(Engine.getInstance)
    sys.addShutdownHook(engineShutdown)
  }

  private def startWebServer = {
    server.foreach(s => s.start())
  }

  private def logConfiguration = {
    val config        = ConfigFactory.load()
    val dbPath        = config.getString("engine.graphdb.path")
    val scheme        = "http"
    val host          = config.getString("engine.communication.webserver.host")
    val port          = config.getString("engine.communication.webserver.port")
    val engineVersion = config.getString("engine.version")
    val configuration = s"""
    |Engine Configuration
    |Version: $engineVersion
    |Database Path: $dbPath
    |WebSocket Server Listening on: $scheme://$host:$port/ws
    """.stripMargin
    logger.info(configuration)
  }

  private def engineShutdown = {
    try{
      logger.info("Shutting down the Engine.")
      server.foreach(s => s.stop())
      // engine.foreach(e => e.shutdown())
      //Another shutdown hook is added for the engine already. In Engine.scala
      //TODO: I need to figure out how to guarrentee that the server stops before the engine.
      logger.info("Engine shutdown complete.")
    }catch{
      case e: Throwable => logger.error("An exception was thrown during shutdown.", e)
    }
  }
}
