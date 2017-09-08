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
import org.machine.engine.TestUtils
import org.machine.engine.Engine
import org.machine.engine.exceptions._
import org.machine.engine.graph.commands.{CommandScopes}
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class WebServerDataSetSpec extends FunSpecLike
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfterEach{
  import WSHelper._
  import TestUtils._
  import LoginHelper._

  //Configure the whenReady for how long to wait.
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val config = ConfigFactory.load()
  private val server = new WebServer()
  private val serviceCreds = serviceCredentials()
  private val PROTOCOL: String = "engine.json.v1"
  private var jwtSessionToken:String = null
  private var activeUserId:String = null
  private val dbPath = config.getString("engine.graphdb.path")
  private val dbFile = new File(dbPath)
  private var engine:Engine = null

  private val scheme = "http"
  private val host = config.getString("engine.communication.webserver.host")
  private val port = config.getString("engine.communication.webserver.port")
  private val engineVersion = config.getString("engine.version")
  private val enginePath = s"ws://$host:$port/ws"
  private val echoPath = s"ws://$host:$port/ws/ping"

  private var starwarsDsId:String = null
  private var hanId:String = null
  private var chewieId:String = null
  private var leiaId:String = null
  private var lukeId:String = null
  private var r2Id:String = null

  /*
  TODO Test the WebServer
  1. Get an instance of the engine.
  2. Create a set of data that can be used for all 36 commands.
  3. Test the execution of all 41 rules (36 commands).
  */
  override def beforeAll(){
    engine = Engine.getInstance
    perge
    server.start()
    val newUserResponse = createUser(serviceCreds)
    activeUserId = getUserId(newUserResponse._2)
    jwtSessionToken = login(serviceCreds)
    createStarWarsSet()
  }

  override def afterAll(){
    server.stop()
    perge
  }

  override def afterEach(){
    engine.reset()
  }

  def createStarWarsSet() = {
    engine.setUser(activeUserId)
    starwarsDsId = engine.createDataSet("Star Wars", "Space Opera")
    val charEDId = engine.onDataSet(starwarsDsId)
      .defineElement("Character", "A person in the movie.")
      .withProperty("Name", "String", "The name of the character.")
    .end

    hanId = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "Han Solo").end
    chewieId = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "Chewbacca").end
    leiaId = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "Princess Leia Organa").end
    lukeId = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "Luke Skywalker").end
    r2Id = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "R2D2").end

    engine.inDataSet(starwarsDsId).attach(leiaId).to(hanId).as("married_to").withField("wears_the_pants", true).end
    engine.inDataSet(starwarsDsId).attach(hanId).to(leiaId).as("married_to").end
    engine.inDataSet(starwarsDsId).attach(leiaId).to(lukeId).as("related_to").withField("wears_the_pants", true).end
    engine.inDataSet(starwarsDsId).attach(lukeId).to(r2Id).as("friends_with").end
  }

  describe("Receiving Requests"){
    describe("WebSocket Requests"){
      describe("Datasets"){
        it ("should CreateElementDefinition By DS ID"){
          val dsId = engine.forUser(activeUserId).createDataSet("dsQ", "DS")
          val edName = "Space Ship"
          val edDesc = "A ship that can traverse outer space."
          val edSpec = Map("dsId"->dsId,
            "name"->edName,
            "description"->edDesc,
            "properties"->Seq(Map("name"->"p1", "propertyType"->"String", "description"->"p1"),
              Map("name"->"p2", "propertyType"->"String", "description"->"p2")))

          val request = buildWSRequest(activeUserId,
            "Create",
            "DataSet",
            "ElementDefinition",
            "None",
            edSpec)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
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
          val datasetId = engine.forUser(activeUserId).createDataSet(dsName, "DS")
          val edName = "Tardis"
          val edDesc = "A ship that can traverse outer space."
          val edSpec = Map("dsName"->dsName,
            "name"->edName,
            "description"->edDesc,
            "properties"->Seq(Map("name"->"p1", "propertyType"->"String", "description"->"p1"),
              Map("name"->"p2", "propertyType"->"String", "description"->"p2")))

          val request = buildWSRequest(activeUserId,
            "Create",
            "DataSet",
            "ElementDefinition",
            "None",
            edSpec)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
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
          val dataset = engine.forUser(activeUserId).findDataSetByName("dsD")
          val opts = Map("dsId" -> dataset.id)

          val request = buildWSRequest(activeUserId,
            "Retrieve",
            "DataSet",
            "ElementDefinition",
            "All",
            opts)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("ElementDefinitions") should equal(true)
            payloadMap("ElementDefinitions").asInstanceOf[List[Map[String, Any]]].length should be >= 1
          }
        }

        it ("should EditElementDefintion By DS ID"){
          val dsId = engine.forUser(activeUserId).createDataSet("temp", "A data set.")
          val edId = engine.forUser(activeUserId).onDataSet(dsId)
            .defineElement("blah", "A poorly named element definition.")
          .end

          val betterName = "Better Name"

          val request = buildWSRequest(activeUserId,
            "Update",
            "DataSet",
            "ElementDefinition",
            "None",
            Map("dsId" -> dsId, "mid"->edId, "name" -> betterName)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.forUser(activeUserId)
              .onDataSet(dsId)
              .findElementDefinitionById(edId)
            ed.name should equal(betterName)
          }
        }

        it ("should EditElementDefintion By DS Name"){
          val dsName = "Murphy"
          val dsId = engine.forUser(activeUserId).createDataSet(dsName, "A data set.")
          val edId = engine.forUser(activeUserId).onDataSet(dsId)
            .defineElement("blah", "A poorly named element definition.")
          .end

          val betterName = "Better Name"

          val request = buildWSRequest(activeUserId,
            "Update",
            "DataSet",
            "ElementDefinition",
            "None",
            Map("dsName" -> dsName, "mid"->edId, "name" -> betterName)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.forUser(activeUserId).onDataSet(dsId).findElementDefinitionById(edId)
            ed.name should equal(betterName)
          }
        }

       it ("should EditElementPropertyDefinition"){
          val dsName = "Monsters"
          val dsId = engine.forUser(activeUserId).createDataSet(dsName, "A collection of monster types.")
          val propName = "Weakness"
          val edId = engine.forUser(activeUserId)
            .onDataSet(dsId)
            .defineElement("Wolfman", "Guy who gets hairy at night.")
            .withProperty(propName, "String", "Fatal Flay")
          .end

          val improvedDesc = "Fatal Flaw"
          val request = buildWSRequest(activeUserId,
            "Update",
            "DataSet",
            "PropertyDefinition",
            "None",
            Map("dsId" -> dsId,
              "mid"->edId,
              "pname" -> propName,
              "description" -> improvedDesc)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.forUser(activeUserId).onDataSet(dsId).findElementDefinitionById(edId)
            ed.properties.length should equal(1)
            val weaknessPropOption = ed.properties.find{ prop => prop.name == propName }
            weaknessPropOption.isEmpty should equal(false)
            weaknessPropOption.get.description should equal(improvedDesc)
          }
        }

        it ("should RemoveElementPropertyDefinition"){
          val dsId = engine.forUser(activeUserId).createDataSet("Aliens", "A collection of alien types.")
          val propName = "Weakness"
          val edId = engine.forUser(activeUserId)
            .onDataSet(dsId)
            .defineElement("Xenomorph", "Guy with personal boundry issues.")
            .withProperty(propName, "String", "Emotionally Insecure")
          .end

          val request = buildWSRequest(activeUserId,
            "Delete",
            "DataSet",
            "PropertyDefinition",
            "None",
            Map("dsId"-> dsId, "mid"->edId, "pname" -> propName)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.forUser(activeUserId)
              .onDataSet(dsId)
              .findElementDefinitionById(edId)
            ed.properties.length should equal(0)
          }
        }

         it ("should DeleteElementDefintion"){
          val dataset = engine.forUser(activeUserId).findDataSetByName("Aliens")
          val edId = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .defineElement("Preditor", "Bit of a bully.")
            .withProperty("Weakness", "String", "Skin Condition")
          .end

          val request = buildWSRequest(activeUserId,
            "Delete",
            "DataSet",
            "ElementDefinition",
            "None",
            Map("dsId" -> dataset.id, "mid" -> edId)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            val expectedIdMsg = "No element definition with ID: %s could be found in dataset: %s".format(edId, dataset.id)
            the [InternalErrorException] thrownBy{
              engine
                .forUser(activeUserId)
                .onDataSet(dataset.id)
                .findElementDefinitionById(edId)
            }should have message expectedIdMsg
          }
        }

        it ("should FindElementDefinitionById"){
          val dataset = engine.forUser(activeUserId).findDataSetByName("Aliens")
          val xenomorph = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .findElementDefinitionByName("Xenomorph")

          val request = buildWSRequest(activeUserId,
            "Retrieve",
            "DataSet",
            "ElementDefinition",
            "ID",
            Map("dsId" -> dataset.id, "mid" -> xenomorph.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("ElementDefinitions") should equal(true)
            val eds = payloadMap("ElementDefinitions").asInstanceOf[List[Map[String, Any]]]
            eds.length should equal(1)
            val ed = eds.head
            ed("name") should equal(xenomorph.name)
            ed("id") should equal(xenomorph.id)
          }
        }

        it ("should FindElementDefinitionByName"){
          val dataset = engine.forUser(activeUserId).findDataSetByName("Aliens")
          val xenomorph = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .findElementDefinitionByName("Xenomorph")

          val request = buildWSRequest(activeUserId,
            "Retrieve",
            "DataSet",
            "ElementDefinition",
            "Name",
            Map("dsId" -> dataset.id, "name" -> xenomorph.name)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("ElementDefinitions") should equal(true)
            val eds = payloadMap("ElementDefinitions").asInstanceOf[List[Map[String, Any]]]
            eds.length should equal(1)
            val ed = eds.head
            ed("name") should equal(xenomorph.name)
            ed("id") should equal(xenomorph.id)
          }
        }

       it ("should CreateElement"){
          val dsId = engine.forUser(activeUserId)
            .createDataSet("Bands", "Interesting Music Groups")

          val edName = "RockBand"
          val edDesc = "A band that likes to roll..."
          val edId = engine.forUser(activeUserId)
            .onDataSet(dsId)
            .defineElement(edName, edDesc)
            .withProperty("Name", "String", "The name of the band.")
            .withProperty("Singer", "String", "A person responsible for singing.")
            .withProperty("LeadGuitarist", "String", "The primary guitarists.")
          .end

          val request = buildWSRequest(activeUserId,
            "Create",
            "DataSet",
            "Element",
            "None",
            Map("dsId" -> dsId,
              "edId" -> edId,
              "Singer" -> "Axl Rose",
              "LeadGuitarist" -> "Slash",
              "Name" -> "Guns N' Roses"
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])

            payloadMap.contains("id") should equal(true)
            val eId = payloadMap("id").asInstanceOf[String]
            val element = engine.forUser(activeUserId)
              .onDataSet(dsId)
              .findElement(eId)

            element.id should equal(eId)
            element.elementType should equal(edName)
            element.elementDescription should equal(edDesc)
            element.fields.size should equal(3)
            element.fields("Singer") should equal("Axl Rose")
            element.fields("LeadGuitarist") should equal("Slash")
            element.fields("Name") should equal("Guns N' Roses")
          }
        }

        it ("should FindElementById"){
          val dataset = engine.forUser(activeUserId).findDataSetByName("Bands")
          val bands = engine.forUser(activeUserId).onDataSet(dataset.id).elements()
          val gnrOpt = bands.find{ prop =>
            prop.fields.contains("Name") &&
            prop.fields("Name") == "Guns N' Roses"
          }

          val request = buildWSRequest(activeUserId,
            "Retrieve",
            "DataSet",
            "Element",
            "ID",
            Map("dsId" -> dataset.id, "mid" -> gnrOpt.get.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("Elements") should equal(true)
            val elementsList = payloadMap("Elements").asInstanceOf[List[Map[String, Any]]]
            elementsList.length should equal(1)
          }
        }

        it("should FindAllElements"){
          val dataset = engine.forUser(activeUserId).findDataSetByName("Bands")
          val ed = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .findElementDefinitionByName("RockBand")

          engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .provision(ed.id)
            .withField("Name", "Daft Punk")
          .end

          val expectedElementsCount = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .elements()
            .length

          val request = buildWSRequest(activeUserId,
            "Retrieve",
            "DataSet",
            "Element",
            "All",
            Map("dsId" -> dataset.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            val elementsList = payloadMap("Elements").asInstanceOf[List[Map[String, Any]]]
            elementsList.length should equal(expectedElementsCount)
          }
        }

        it ("should EditElement"){
          val dsId = engine.forUser(activeUserId)
            .createDataSet("Un-natural Disasters", "Epic Events of Calamity")
          val edId = engine.forUser(activeUserId)
            .onDataSet(dsId)
            .defineElement("Disaster", "Oh No!")
            .withProperty("Name", "String", "The name of the disaster.")
          .end

          val zaId = engine.forUser(activeUserId).onDataSet(dsId).provision(edId).withField("Name", "Zombie Attack!").end()
          val maId = engine.forUser(activeUserId).onDataSet(dsId).provision(edId).withField("Name", "Monster Attack!").end()
          val zimID = engine.forUser(activeUserId).onDataSet(dsId).provision(edId).withField("Name", "Martians!").end()

          val updatedField = "Invader Zim Attack!"
          val request = buildWSRequest(activeUserId,
            "Update",
            "DataSet",
            "Element",
            "None",
            Map("dsId" -> dsId,
              "elementId" -> zimID,
              "Name" -> updatedField)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zimID)

            //get the ID and verify it is zimID.
            //Using the engine to get the element and verify it has been updated.
            val martian = engine.forUser(activeUserId).onDataSet(dsId).findElement(zimID)
            martian.field[String]("Name") should equal(updatedField)
          }
        }

        it ("should AddElementField"){
          val dataset = engine.forUser(activeUserId)
            .findDataSetByName("Un-natural Disasters")
          val elements = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .elements()
          val zombieAttack = elements.find(e => e.field[String]("Name") == "Zombie Attack!").get
          zombieAttack.fields.size should equal(1)

          val request = buildWSRequest(activeUserId,
            "Update",
            "DataSet",
            "Element",
            "None",
            Map("dsId" -> dataset.id,
              "elementId" -> zombieAttack.id,
              "DeadlyRanking" -> "4")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zombieAttack.id)
            val updatedElement = engine.forUser(activeUserId)
              .onDataSet(dataset.id)
              .findElement(zombieAttack.id)
            updatedElement.fields.size should equal(2)
          }
        }

        it ("should RemoveElementField"){
          val dataset = engine.forUser(activeUserId).findDataSetByName("Un-natural Disasters")
          val elements = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .elements()
          val zombieAttack = elements.find(e => e.field[String]("Name") == "Zombie Attack!").get
          zombieAttack.fields.size should equal(2)

          val request = buildWSRequest(activeUserId,
            "Delete",
            "DataSet",
            "ElementField",
            "None",
            Map("dsId" -> dataset.id,
              "elementId" -> zombieAttack.id,
              "DeadlyRanking" -> "DeadlyRanking")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zombieAttack.id)
            val updatedElement = engine.forUser(activeUserId)
              .onDataSet(dataset.id)
              .findElement(zombieAttack.id)
            updatedElement.fields.size should equal(1)
          }
        }

        it ("should DeleteElement"){
          val dataset = engine.forUser(activeUserId)
            .findDataSetByName("Un-natural Disasters")
          val elements = engine.forUser(activeUserId)
            .onDataSet(dataset.id)
            .elements()
          elements.length should equal(3)
          val zombieAttack = elements.find(e => e.field[String]("Name") == "Zombie Attack!").get

          val request = buildWSRequest(activeUserId,
            "Delete",
            "DataSet",
            "Element",
            "None",
            Map("dsId" -> dataset.id,
              "elementId" -> zombieAttack.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zombieAttack.id)
            val updatedElementList = engine.forUser(activeUserId)
              .onDataSet(dataset.id)
              .elements
            updatedElementList.length should equal(2)
          }
        }

        it ("should AssociateElements"){
          val associationType = "friends_with"
          val request = buildWSRequest(activeUserId,
            "Create",
            "DataSet",
            "Association",
            "None",
            Map("dsId"    -> starwarsDsId,
              "startingElementId" -> hanId,
              "endingElementId"   -> chewieId,
              "associationName"   -> associationType,
              "friendshipType"    -> "best buds"
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])

            payloadMap.contains("id") should equal(true)
            val assocId           = payloadMap("id").toString
            val friendshipEternal = engine.forUser(activeUserId).onDataSet(starwarsDsId).findAssociation(assocId)

            friendshipEternal.id                should equal(assocId)
            friendshipEternal.associationType   should equal(associationType)
            friendshipEternal.startingElementId should equal(hanId)
            friendshipEternal.endingElementId   should equal(chewieId)
            friendshipEternal.fields.size       should equal(1)
          }
        }

        it ("should FindAssociationById"){
          val associations = engine.forUser(activeUserId)
            .onDataSet(starwarsDsId)
            .onElement(hanId)
            .findOutboundAssociations()
          associations.length should equal(2)

          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get

          val request = buildWSRequest(activeUserId,
            "Retrieve",
            "DataSet",
            "Association",
            "ID",
            Map(
              "associationId" -> friendsWith.id
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("Associations") should equal(true)
            val foundAssociations    = payloadMap("Associations").asInstanceOf[List[Map[String, Any]]]
            foundAssociations.length should equal(1)
            val foundAssociation     = foundAssociations.head
            foundAssociation("startingElementId")        should equal(hanId)
            foundAssociation.contains("endingElementId") should equal(true)
            foundAssociation("associationType")          should equal(friendsWith.associationType)
            foundAssociation("id")                       should equal(friendsWith.id)
            foundAssociation("fields").asInstanceOf[List[Map[String, Any]]].size should equal(1)
          }
        }

        it ("should EditAssociation"){
          val associations = engine.forUser(activeUserId)
            .onDataSet(starwarsDsId)
            .onElement(hanId)
            .findOutboundAssociations()

          associations.length should equal(2)
          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get

          val request = buildWSRequest(activeUserId,
            "Update",
            "DataSet",
            "Association",
            "None",
            Map(
              "associationId"  -> friendsWith.id,
              "friendshipType" -> "meaningful",
              "duration"       -> "A long time."
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(friendsWith.id)

            val updatedAssoc = engine.forUser(activeUserId)
              .onDataSet(starwarsDsId)
              .findAssociation(friendsWith.id)
            updatedAssoc.fields.size should equal(2)
            updatedAssoc.field[String]("friendshipType") should equal("meaningful")
            updatedAssoc.field[String]("duration") should equal("A long time.")
          }
        }

        it ("should RemoveAssociationField"){
          val associations = engine.forUser(activeUserId)
            .onDataSet(starwarsDsId)
            .onElement(hanId).findOutboundAssociations()
          associations.length should equal(2)
          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get
          friendsWith.fields.size should equal(2)

          val request = buildWSRequest(activeUserId,
            "Delete",
            "DataSet",
            "AssociationField",
            "None",
            Map(
              "associationId"  -> friendsWith.id,
              "duration"       -> "duration"
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(friendsWith.id)
            val updatedAssoc = engine.forUser(activeUserId)
              .onDataSet(starwarsDsId)
              .findAssociation(friendsWith.id)
            updatedAssoc.fields.size should equal(1)
            updatedAssoc.field[String]("friendshipType") should equal("meaningful")
          }
        }

        it ("should DeleteAssociation"){
          val associations = engine.forUser(activeUserId)
            .onDataSet(starwarsDsId)
            .onElement(hanId)
            .findOutboundAssociations()
          associations.length should equal(2)
          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get

          val request = buildWSRequest(activeUserId,
            "Delete",
            "DataSet",
            "Association",
            "None",
            Map(
              "associationId"  -> friendsWith.id
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(friendsWith.id)

            val expectedMsg = "No association with associationId: %s could be found.".format(friendsWith.id)
            the [InternalErrorException] thrownBy{
              engine.findAssociation(friendsWith.id)
            }should have message expectedMsg
          }
        }

    /*    it ("should FindInboundAssociationsByElementId"){
          val associations = engine.onDataSet(starwarsDsId).onElement(hanId).findInboundAssociations()
          associations.length should equal(1) //From Leia

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="InboundAssociation",
            filter="None",
            options=Map(
              "elementId"  -> hanId
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("Associations") should equal(true)
            val assocs = payloadMap("Associations").asInstanceOf[List[Map[String, Any]]]
            assocs.length should equal(1)
            assocs.head("associationType").toString should equal("married_to")
          }
        }

        it ("should FindOutboundAssociationsByElementId"){
          val associations = engine.onDataSet(starwarsDsId).onElement(leiaId).findOutboundAssociations()
          associations.length should equal(2) //To Han & Luke

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="OutboundAssociation",
            filter="None",
            options=Map(
              "elementId"  -> leiaId
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("Associations") should equal(true)
            val assocs = payloadMap("Associations").asInstanceOf[List[Map[String, Any]]]
            assocs.length should equal(2) //Han & Luke
          }
        }


        // import org.machine.engine.viz.GraphVizHelper
        // GraphVizHelper.visualize(engine.database)

        it ("should FindDownStreamElementsByElementId"){
          val downstream = engine.onDataSet(starwarsDsId).onElement(lukeId).findDownStreamElements()
          downstream.length should equal(1)
          val r2 = downstream.head
          r2.field[String]("Name") should equal("R2D2")

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="Element",
            filter="Downstream",
            options=Map(
              "elementId"  -> lukeId
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("status") should equal(true)
            payloadMap("status").toString should equal("OK")
            payloadMap.contains("Elements") should equal(true)
            val elements = payloadMap("Elements").asInstanceOf[List[Map[String, Any]]]
            elements.length should equal(1)
            elements.head("id") should equal(r2.id)
          }
        }

        it ("should FindUpStreamElementsByElementId"){
          val upstream = engine.onDataSet(starwarsDsId).onElement(lukeId).findUpStreamElements()
          upstream.length should equal(1)
          val leia = upstream.head
          leia.field[String]("Name") should equal("Princess Leia Organa")

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="Element",
            filter="Upstream",
            options=Map(
              "elementId"  -> lukeId
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("status") should equal(true)
            payloadMap("status").toString should equal("OK")
            payloadMap.contains("Elements") should equal(true)
            val elements = payloadMap("Elements").asInstanceOf[List[Map[String, Any]]]
            elements.length should equal(1)
            elements.head("id") should equal(leia.id)
          }
        }

        it ("should RemoveInboundAssociations"){
          engine.onDataSet(starwarsDsId).onElement(hanId)
            .findInboundAssociations().length should equal(1)

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="InboundAssociation",
            filter="None",
            options=Map(
              "elementId"  -> hanId
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("status") should equal(true)
            payloadMap("status").toString should equal("OK")
            engine.onDataSet(starwarsDsId).onElement(hanId)
              .findInboundAssociations().length should equal(0)
          }
        }

        it ("should RemoveOutboundAssociations"){
          engine.onDataSet(starwarsDsId).onElement(hanId)
            .findOutboundAssociations().length should equal(1) //To Leia

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="OutboundAssociation",
            filter="None",
            options=Map(
              "elementId"  -> hanId
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath, PROTOCOL, jwtSessionToken)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.head))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("status") should equal(true)
            payloadMap("status").toString should equal("OK")
            engine.onDataSet(starwarsDsId).onElement(hanId)
              .findOutboundAssociations().length should equal(0)
          }
        }
        */
      }
    }
  }

  def getUserId(newUserResponse: String):String = {
    val responseMap = strToMap(newUserResponse)
    val userId = responseMap.get("userId").get.toString()
    return userId
  }
}
