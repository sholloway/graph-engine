package org.machine.engine

import akka.actor.{ ActorSystem, Props, Terminated}
import akka.camel.{CamelMessage, Consumer}
import com.typesafe.config._
import com.typesafe.scalalogging.LazyLogging
import org.machine.engine.communication.WebServer
import org.machine.engine.exceptions._
import scala.concurrent.Future
/*
Challenges
Configuration that needs to live outside of the Jar.
- User Settings
- Where the database should be. Right now it's target/Engine.graph This doesn't make sense.
*/
object Main extends LazyLogging{
  private implicit var engine:Option[Engine] = None
  private implicit var server:Option[WebServer] = None

  def main(args: Array[String]):Unit = {
    logger.info("Starting up the Engine.")
    try{
      logConfiguration
      initializeWebServer
      initializeGraphEngine
      startWebServer
      listenToStdin
    }catch{
      case e: Throwable => {
        logger.error("An exception was thrown during startup.", e)
        System.exit(1)
      }
    }
    // System.exit(0)
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
      //I need to figure out how to guarrentee that the server stops before the engine.
      logger.info("Engine shutdown complete.")
    }catch{
      case e: Throwable => logger.error("An exception was thrown during shutdown.", e)
    }
  }

  private def listenToStdin = {
    val system = ActorSystem("std-actor-system")
    val terminatedFuture:Future[Terminated] = system.whenTerminated
    import scala.concurrent.ExecutionContext.Implicits.global
    terminatedFuture onSuccess {
      case ok =>{
        logger.info("std-actor-system was succesfully terminated. Terminating the JVM.")
        System.exit(0)
      }
    }

    terminatedFuture onFailure {
      case err =>{
        logger.error("std-actor-system generated an error while attempting to terminate.", err)
        logger.error("Shutting down the JVM in error state.")
        System.exit(1)
      }
    }

    val stdinConsumer = system.actorOf(Props[StdinConsumer])


  }
}

/** Responsible for communicating with the Node.js Harness.
Communication with Node.js should be over stdin, stdout and stderr.
*/
class StdinConsumer extends Consumer{
  def endpointUri = "stream:in"  

  override def preStart() {
    System.out.println("ENGINE_READY")
  }

  override def postStop() = {
    System.out.println("Engine StdinConsumer was stopped")
  }

  def receive = {
    case msg: CamelMessage => {
      val txtMsg = msg.bodyAs[String]
      context.system.log.info(s"Engine StdinConsumer recieved message: $txtMsg")
      System.out.println(s"Engine StdinConsumer recieved message: $txtMsg")
      txtMsg match{
        case "SIGHUP" => {
          context.system.terminate()
        }
        case _ => {
          context.system.log.info(s"Engine StdinConsumer received unknown message.")
          System.out.println(s"Engine StdinConsumer received unknown message.")
        }
      }
      System.out.println(s"Engine received $txtMsg")
    }
    case _ => {
      context.system.log.error("Stdin passed a message that could not be handled.")
      System.out.println("Stdin passed a message that could not be handled.")
    }
  }
}
