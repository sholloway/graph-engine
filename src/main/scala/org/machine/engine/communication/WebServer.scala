package org.machine.engine.communication

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Graph, FlowShape}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.collection.JavaConversions._
import scala.concurrent.{Future}

import org.machine.engine.communication.routes.{IdentityServiceRouteBuilder, RPCOverWSRouteBuilder, WebSocketRouteBuilder}

class WebServer {
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap.
  private implicit val executionContext = system.dispatcher
  private val config = system.settings.config

  private var bindingFutureOption:Option[Future[Http.ServerBinding]] = None;
  private var idServBindingFutureOption:Option[Future[Http.ServerBinding]] = None;

  def start() = {
    bindingFutureOption = initializeWebSocketsEndpoint()
    idServBindingFutureOption = initializeIdentityServiceEndpoint()
  }

  def stop() = {
    system.log.info("Webserver Stopping")
    bindingFutureOption.foreach{ future =>
      future.flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
    }

    idServBindingFutureOption.foreach{ future =>
      future.flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
    }
  }

  private def initializeWebSocketsEndpoint():Option[Future[Http.ServerBinding]] = {
    system.log.info("Webserver Starting")
    val wsRoutes = RPCOverWSRouteBuilder.buildRoutes()
    val wsHost = config.getString("engine.communication.webserver.host")
    val wsPort = config.getInt("engine.communication.webserver.port")
    return Some(Http().bindAndHandle(wsRoutes, wsHost, wsPort))
  }

  private def initializeIdentityServiceEndpoint():Option[Future[Http.ServerBinding]] = {
    system.log.info("Identity Service Starting")
    val identityServiceRoutes = IdentityServiceRouteBuilder.buildRoutes()
    val idHost = config.getString("engine.communication.identity_service.host")
    val idPort = config.getInt("engine.communication.identity_service.port")
    return Some(Http().bindAndHandle(identityServiceRoutes, idHost, idPort))
  }
}
