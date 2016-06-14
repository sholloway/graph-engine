package org.machine.engine.communication

import java.io.File;
import java.io.IOException;
import org.neo4j.io.fs.FileUtils

import org.scalatest._
import org.scalatest.mock._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import akka.actor.ActorSystem
import akka.actor.{Actor, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config._
import org.machine.engine.Engine
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class WebServerSystemSpaceSpec extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll{
  import WSHelper._

  //Configure the whenReady for how long to wait.
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val config = ConfigFactory.load()
  val server = new WebServer()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null

  val scheme = "http"
  val host = config.getString("engine.communication.webserver.host")
  val port = config.getString("engine.communication.webserver.port")
  val engineVersion = config.getString("engine.version")
  val enginePath = s"ws://$host:$port/ws"
  val echoPath = s"ws://$host:$port/ws/ping"

  /*
  TODO Test the WebServer
  1. Get an instance of the engine.
  2. Create a set of data that can be used for all 36 commands.
  3. Test the execution of all 41 rules (36 commands).
  */
  override def beforeAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
    engine = Engine.getInstance
    server.start()
  }

  override def afterAll(){
    server.stop()
    // TestKit.shutdownActorSystem(system)
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Receiving Requests"){
    describe("WebSocket Requests"){
      describe("System Space"){
        it ("should create element definition"){
          val edSpec = Map("name"->"Mobile Device",
            "description"->"A computer that can be carried by the user.",
            "properties"->Seq(Map("name"->"Model", "propertyType"->"String", "description"->"The specific manufacture model."),
              Map("name"->"Manufacture", "propertyType"->"String", "description"->"The company that made the device.")))

          val request = buildWSRequest(user="Bob",
            actionType="Create",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="None",
            options=edSpec)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            val edId = payloadMap("id").asInstanceOf[String]
            val ed = engine.inSystemSpace.findElementDefinitionById(edId)
            ed.name should equal("Mobile Device")
            ed.description should equal("A computer that can be carried by the user.")
            ed.properties should have length 2
          }
        }

        it ("should list all element definitions"){
          engine
            .inSystemSpace
            .defineElement("Note", "A brief record of something, captured to assist the memory or for future reference.")
            .withProperty("Note Text", "String", "The body of the note.")
          .end

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="All")

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("ElementDefinitions") should equal(true)
            payloadMap("ElementDefinitions").asInstanceOf[List[Map[String, Any]]].length should be >= 1
          }
        }

        it ("should provide an empty payload if no element definitions exist"){
          purgeAllElementDefinitions()

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="All")

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payload = envelopeMap("textMessage").asInstanceOf[String]
            payload.isEmpty should equal(true)
          }
        }

        it ("should EditElementDefintion"){
          purgeAllElementDefinitions()
          val edId = createTimepieceElementDefinition()

          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="None",
            options=Map("mid"->edId, "name" -> "Watch")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.inSystemSpace.findElementDefinitionById(edId)
            ed.name should equal("Watch")
          }
        }

        /*
        Next Steps
        Finish this test.
        1. Create an ED
        2. Through the WS API editity a property on the ED.
        3. Verify through the Engine API the change occured.

        FIXME There is no way to add properties to an existing ElementDefinition.
        FIXME The code flow is confusing.
        1. RequestMessage.jsonToMap (DeserializeClientMessage)
        2. RequestRuleValidator.validate (DeserializeClientMessage)
        3. RequestMessage.parseJSON (DeserializeClientMessage)
        4. buildOptions(EngineWorkerPoolManager)

        Could we get rid of the initial jsonToMap?
        */
        it ("should EditElementPropertyDefinition"){
          purgeAllElementDefinitions()
          val edId = createTimepieceElementDefinition()

          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="SystemSpace",
            entityType="PropertyDefinition",
            filter="None",
            options=Map("mid"->edId, "pname" -> "Hours",
              "description" -> "Tracks the passing of hours.")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.inSystemSpace.findElementDefinitionById(edId)
            ed.properties.length should equal(3)
            val hoursPropOption = ed.properties.find{ prop => prop.name == "Hours" }
            hoursPropOption.isEmpty should equal(false)
            hoursPropOption.get.description should equal("Tracks the passing of hours.")
          }
        }

        it ("should RemoveElementPropertyDefinition"){
          purgeAllElementDefinitions()
          val edId = createTimepieceElementDefinition()

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="SystemSpace",
            entityType="PropertyDefinition",
            filter="None",
            options=Map("mid"->edId, "pname" -> "Hours")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.inSystemSpace.findElementDefinitionById(edId)
            ed.properties.length should equal(2)
            val hoursPropOption = ed.properties.find{ prop => prop.name == "Hours" }
            hoursPropOption.isEmpty should equal(true)
          }
        }

        it ("should DeleteElementDefintion"){
          purgeAllElementDefinitions()
          val edId = createTimepieceElementDefinition()

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="None",
            options=Map("mid"->edId)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            val expectedIdMsg = "No element with ID: %s could be found in %s".format(edId, "internal_system_space")
            the [InternalErrorException] thrownBy{
              engine
                .inSystemSpace
                .findElementDefinitionById(edId)
            }should have message expectedIdMsg
          }
        }

        it ("should FindElementDefinitionById"){
          purgeAllElementDefinitions()
          val edId = createTimepieceElementDefinition()

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="ID",
            options=Map("mid"->edId)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("ElementDefinitions") should equal(true)
            val eds = payloadMap("ElementDefinitions").asInstanceOf[List[Map[String, Any]]]
            eds.length should equal(1)
            val ed = eds.head
            ed("name") should equal("Timepiece")
            ed("id") should equal(edId)
          }
        }

        it ("should FindElementDefinitionByName"){
          purgeAllElementDefinitions()
          val edId = createTimepieceElementDefinition()

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="Name",
            options=Map("name"->"Timepiece")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("ElementDefinitions") should equal(true)
            val eds = payloadMap("ElementDefinitions").asInstanceOf[List[Map[String, Any]]]
            eds.length should equal(1)
            val ed = eds.head
            ed("name") should equal("Timepiece")
            ed("id") should equal(edId)
          }
        }
      }
    }
  }
}
