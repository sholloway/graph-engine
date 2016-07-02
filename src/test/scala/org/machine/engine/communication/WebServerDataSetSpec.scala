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
import org.machine.engine.graph.commands.{CommandScopes}
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class WebServerDataSetSpec extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll{
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
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Receiving Requests"){
    describe("WebSocket Requests"){
      describe("Datasets"){
        it ("should CreateElementDefinition By DS ID"){
          val dsId = engine.createDataSet("dsQ", "DS")
          val edName = "Space Ship"
          val edDesc = "A ship that can traverse outer space."
          val edSpec = Map("dsId"->dsId,
            "name"->edName,
            "description"->edDesc,
            "properties"->Seq(Map("name"->"p1", "propertyType"->"String", "description"->"p1"),
              Map("name"->"p2", "propertyType"->"String", "description"->"p2")))

          val request = buildWSRequest(user="Bob",
            actionType="Create",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="None",
            options=edSpec)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            val edId = payloadMap("id").asInstanceOf[String]
            val ed = engine.onDataSet(dsId).findElementDefinitionById(edId)
            ed.name should equal(edName)
            ed.description should equal(edDesc)
            ed.properties should have length 2
          }
        }

        it ("should CreateElementDefinition By DS Name"){
          val dsName = "dsD"
          val datasetId = engine.createDataSet(dsName, "DS")
          val edName = "Tardis"
          val edDesc = "A ship that can traverse outer space."
          val edSpec = Map("dsName"->dsName,
            "name"->edName,
            "description"->edDesc,
            "properties"->Seq(Map("name"->"p1", "propertyType"->"String", "description"->"p1"),
              Map("name"->"p2", "propertyType"->"String", "description"->"p2")))

          val request = buildWSRequest(user="Bob",
            actionType="Create",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="None",
            options=edSpec)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            val edId = payloadMap("id").asInstanceOf[String]
            try{
              val ed = engine.onDataSet(datasetId).findElementDefinitionById(edId)
              ed.name should equal(edName)
              ed.description should equal(edDesc)
              ed.properties should have length 2
            }catch{
              case e: InternalErrorException => {
                println(e)
                fail()
              }
            }
          }
        }

        it ("should ListAllElementDefinitions"){
          val dataset = engine.findDataSetByName("dsD")

          val opts = Map("dsId" -> dataset.id)

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="All",
            options=opts)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("ElementDefinitions") should equal(true)
            payloadMap("ElementDefinitions").asInstanceOf[List[Map[String, Any]]].length should be >= 1
          }
        }

//  import org.machine.engine.viz.GraphVizHelper
        it ("should EditElementDefintion By DS ID"){
          val dsId = engine.createDataSet("temp", "A data set.")
          val edId = engine.onDataSet(dsId)
            .defineElement("blah", "A poorly named element definition.")
          .end

          val betterName = "Better Name"

          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="None",
            options=Map("dsId" -> dsId, "mid"->edId, "name" -> betterName)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.onDataSet(dsId).findElementDefinitionById(edId)
            ed.name should equal(betterName)
          }
        }

        it ("should EditElementDefintion By DS Name"){
          val dsName = "Murphy"
          val dsId = engine.createDataSet(dsName, "A data set.")
          val edId = engine.onDataSet(dsId)
            .defineElement("blah", "A poorly named element definition.")
          .end

          val betterName = "Better Name"

          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="None",
            options=Map("dsName" -> dsName, "mid"->edId, "name" -> betterName)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.onDataSet(dsId).findElementDefinitionById(edId)
            ed.name should equal(betterName)
          }
        }

        it ("should EditElementPropertyDefinition"){
          val dsName = "Monsters"
          val dsId = engine.createDataSet(dsName, "A collection of monster types.")
          val propName = "Weakness"
          val edId = engine.onDataSet(dsId)
            .defineElement("Wolfman", "Guy who gets hairy at night.")
            .withProperty(propName, "String", "Fatal Flay")
          .end

          val improvedDesc = "Fatal Flaw"
          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="DataSet",
            entityType="PropertyDefinition",
            filter="None",
            options=Map("dsId" -> dsId,
              "mid"->edId,
              "pname" -> propName,
              "description" -> improvedDesc)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.onDataSet(dsId).findElementDefinitionById(edId)
            ed.properties.length should equal(1)
            val weaknessPropOption = ed.properties.find{ prop => prop.name == propName }
            weaknessPropOption.isEmpty should equal(false)
            weaknessPropOption.get.description should equal(improvedDesc)
          }
        }

        it ("should RemoveElementPropertyDefinition")(pending)
        it ("should DeleteElementDefintion")(pending)
        it ("should FindElementDefinition")(pending)
        it ("should FindElementDefinitionById")(pending)
        it ("should FindElementDefinitionByName")(pending)

        it ("should CreateElement")(pending)
        it ("should DeleteAssociation")(pending)
        it ("should DeleteElement")(pending)

        it ("should AssociateElements")(pending)
        it ("should EditAssociation")(pending)
        it ("should RemoveInboundAssociations")(pending)
        it ("should RemoveOutboundAssociations")(pending)

        it ("should EditElement")(pending)

        it ("should FindDownStreamElementsByElementId")(pending)
        it ("should FindElementById")(pending)

        it ("should FindAssociationById")(pending)
        it ("should FindInboundAssociationsByElementId")(pending)
        it ("should FindOutboundAssociationsByElementId")(pending)
        it ("should FindUpStreamElementsByElementId")(pending)
        it ("should RemoveAssociationField")(pending)
        it ("should RemoveElementField")(pending)
      }
    }
  }
}
