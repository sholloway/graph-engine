package org.machine.engine.communication

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers.{ BasicHttpCredentials, Authorization }
import akka.util.ByteString
import net.liftweb.json._
import net.liftweb.json.DefaultFormats

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.concurrent.PatienceConfiguration.Interval

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

import org.machine.engine.Engine
import org.machine.engine.flow.requests._
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.{CommandScope, CommandScopes}

object WSHelper{
  import Matchers._
  import ScalaFutures._
  import HttpMethods._
  import Neo4JHelper._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  import system.dispatcher

  def getHTTP(url:String):Future[HttpResponse] = {
    return Http().singleRequest(HttpRequest(GET, uri = url))
  }

  def normalize(msg: String):String = {
    msg.replaceAll("\t","").replaceAll("\n","").replaceAll(" ", "")
  }

  def verifyHTTPRequest(responseFuture: Future[HttpResponse],
    expected: String, timeout: Span, log: Boolean = false) = {
    whenReady(responseFuture, new Interval(timeout)) { response =>
      response match {
        case HttpResponse(StatusCodes.OK, headers, entity, _) => {
          val bs: Future[ByteString] = entity.toStrict(FiniteDuration(5, "seconds")).map { _.data }
          val s: Future[String] = bs.map(_.utf8String)
          whenReady(s){ payload =>
            if(log){
              println(payload)
            }
            normalize(payload) should equal(normalize(expected))
          }
        }
        case HttpResponse(code, _, _, _) =>
          println("Request failed, response code: " + code)
          fail()
      }
    }
  }

  def buildWSRequest(user: String,
    actionType: String,
    scope: String,
    entityType: String,
    filter: String, options: Map[String, Any] = Map.empty[String, Any]):Source[Message, NotUsed] = {
    val rm = RequestMessage(user, actionType, scope, entityType, filter, options)
    val json = RequestMessage.toJSON(rm)
    return Source.single(TextMessage(json))
  }

  /*
  Invoke the websocket and return a future with the Sink
  captured as a sequence of responses.
  */
  def invokeWS(request: Source[Message, NotUsed],
    path: String,
    protocol: String = "engine.json.v1"):Future[Seq[Message]] = {
    val flow = Flow.fromSinkAndSourceMat(Sink.seq[Message], request)(Keep.left)
    val (upgradeResponse, closed) = Http().singleWebSocketRequest(WebSocketRequest(path, subprotocol = Some(protocol)), flow)
    val connected = upgradeResponse.map(verifyProtocolsSwitched)
    connected.onFailure(failTest)
    return closed
  }

  def msgToMap(msg: Message):Map[String, Any] = {
    val txtMessage = msg.asInstanceOf[TextMessage.Strict]
    val envelopeDom = parse(txtMessage.text)
    return envelopeDom.values.asInstanceOf[Map[String, Any]]
  }

  def strToMap(str: String):Map[String, Any] = {
    val payloadDom = parse(str)
    return payloadDom.values.asInstanceOf[Map[String, Any]]
  }

  def printJson(request: RequestMessage) = {
    import net.liftweb.json.JsonAST._
    import net.liftweb.json.Extraction._
    import net.liftweb.json.Printer._
    implicit val formats = net.liftweb.json.DefaultFormats
    println(prettyRender(decompose(request)))
  }

  def tm(msg: String):Message = TextMessage(msg)

  def createPrintSink(): Sink[Message, Future[Done]] = Sink.foreach {
    case message: TextMessage.Strict => {
      println("Received Response from Server:")
      println(message.text)
    }
  }

  def verifyProtocolsSwitched(upgrade: WebSocketUpgradeResponse): Done = {
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Done
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  val failTest:PartialFunction[Throwable, Any] = {
    case e => {
      println(e)
      Matchers.fail()
    }
  }

  /*
  Deletes all element definitions.
  WARNING: Don't run on a data set you care about.
  */
  def purgeAllElementDefinitions(scope: CommandScope = CommandScopes.SystemSpaceScope) = {
    val eds = Engine.getInstance.setScope(scope).elementDefinitions
    eds.foreach{ ed =>
      Engine.getInstance
        .setScope(scope)
        .onElementDefinition(ed.id)
        .delete
      .end
    }
  }

  def createTimepieceElementDefinition(scope: CommandScope = CommandScopes.SystemSpaceScope):String = {
    val edId = Engine.getInstance
      .setScope(scope)
      .defineElement("Timepiece", "A time tracking apparatus.")
      .withProperty("Hours", "Int", "Tracks the passing.")
      .withProperty("Minutes", "Int", "Tracks the passing of minutes.")
      .withProperty("Seconds", "Int", "Tracks the passing of seconds.")
    .end
    return edId
  }

  def validateOkMsg(envelopeMap: Map[String, Any]):Map[String, Any] = {
    envelopeMap("status") should equal("Ok")
    envelopeMap("messageType") should equal("CmdResult")
    return envelopeMap
  }
}
