package org.machine.communication.harness

import akka.actor.{ ActorSystem, Props, Terminated}
import akka.camel.{CamelMessage, Consumer, Producer}
import scala.concurrent.Future
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.actor.{ActorPublisher, ActorSubscriber, ActorSubscriberMessage, OneByOneRequestStrategy}
import akka.stream.{ActorMaterializer}
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}

/** Responsible for communicating with the application harness over STDIN and STDOUT.
==Design==
The design combines Akka Streams with Akka Camel.
Client -> STDIN Consumer Source -> Subscriber Sink
                                        |
                                        v
Client <------------------------- STDOUT Producer
This should be used for boot strapping the engine, but not for
application level communication. That should be handled by the
Web Socket endpoint.
*/
object STDEngineHarness extends LazyLogging{
  /** Initializes the STDIN/STDOUT stream.
  Consumes from STDIN and returns messages on STDOUT.
  */
  def startStdinHarness = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("std-actor-system")
    implicit val materializer = ActorMaterializer()
    val terminatedFuture:Future[Terminated] = system.whenTerminated

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

    val source = Source.actorPublisher[String](Props[StdinConsumer])
    val sink = Sink.actorSubscriber[String](Props[StdinSubscriber])

    source.map(_.toUpperCase).
      to(sink).
      run()
  }

  /**
  Communication with Node.js should be over stdin, stdout and stderr.
  BUG: I can't figure out why things are getting capitalied in their response.
  BUG: Not logging to file.
  ==Message Types==
  SIGHUP: Shuts down the system.
  */
  class StdinConsumer extends Consumer with ActorPublisher[String] with StrictLogging{
    def endpointUri = "stream:in"
    import akka.stream.actor.ActorPublisherMessage._

    def receive = {
      case msg: CamelMessage =>
        msg.bodyAs[String] match {
          case "SIGHUP" => {
            logger.info("CamelConsumer: Received SIGHUP")
            onComplete()
          }
          case txt if (totalDemand > 0) =>
            logger.info(s"CamelConsumer: Received ${txt}")
            onNext(txt)
        }
      case Request(_) => //ignored
      case Cancel =>
        context.stop(self)
    }
  }

  /** Directs all incoming messages to STDOUT.
  */
  class StdoutProducer extends Producer {
    def endpointUri = "stream:out"
  }

  /** Process inbound messages. Directs responses to StdoutProducer.
  */
  class StdinSubscriber extends ActorSubscriber with StrictLogging{
    import ActorSubscriberMessage._
    override val requestStrategy = OneByOneRequestStrategy
    val endPoint = context.actorOf(Props[StdoutProducer])

    override def preStart() = {
      logger.debug(s"Engine CamelSubscriber preStart complete.")
      endPoint ! "ENGINE_READY"
    }

    override def postStop() = {
      logger.info(s"Engine CamelSubscriber postStop complete.")
    }

    def receive = {
      case OnComplete  => {
        logger.debug("Engine CamelSubscriber: Received OnComplete. Initiating Actor System Shutdown")
        context.system.terminate()
      }
      case OnNext(msg: String) => {
        logger.debug(s"CamelSubscriber: Recieved OnNext(${msg})")
        endPoint ! msg
      }
      case _ => {
        logger.error("Stdin passed a message that could not be handled.")
      }
    }
  }
}
