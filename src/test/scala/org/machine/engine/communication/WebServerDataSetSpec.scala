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

  var starwarsDsId:String = null
  var hanId:String = null
  var chewieId:String = null
  var leiaId:String = null

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
    createStarWarsSet()
    server.start()
  }

  override def afterAll(){
    server.stop()
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  def createStarWarsSet() = {
    starwarsDsId = engine.createDataSet("Star Wars", "Space Opera")
    val charEDId = engine.onDataSet(starwarsDsId)
      .defineElement("Character", "A person in the movie.")
      .withProperty("Name", "String", "The name of the character.")
    .end

    hanId = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "Han Solo").end
    chewieId = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "Chewbacca").end
    leiaId = engine.onDataSet(starwarsDsId).provision(charEDId).withField("Name", "Princess Leia Organa").end
    engine.inDataSet(starwarsDsId).attach(leiaId).to(hanId).as("married_to").withField("wears_the_pants", true).end
    engine.inDataSet(starwarsDsId).attach(hanId).to(leiaId).as("married_to").end
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

        it ("should RemoveElementPropertyDefinition"){
          val dsId = engine.createDataSet("Aliens", "A collection of alien types.")
          val propName = "Weakness"
          val edId = engine.onDataSet(dsId)
            .defineElement("Xenomorph", "Guy with personal boundry issues.")
            .withProperty(propName, "String", "Emotionally Insecure")
          .end

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="PropertyDefinition",
            filter="None",
            options=Map("dsId"-> dsId, "mid"->edId, "pname" -> propName)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            //verify with engine that it has changed. :)
            val ed = engine.onDataSet(dsId).findElementDefinitionById(edId)
            ed.properties.length should equal(0)
          }
        }

        it ("should DeleteElementDefintion"){
          val dataset = engine.findDataSetByName("Aliens")
          val edId = engine.onDataSet(dataset.id)
            .defineElement("Preditor", "Bit of a bully.")
            .withProperty("Weakness", "String", "Skin Condition")
          .end

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="None",
            options=Map("dsId" -> dataset.id, "mid" -> edId)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)

            val expectedIdMsg = "No element definition with ID: %s could be found in dataset: %s".format(edId, dataset.id)
            the [InternalErrorException] thrownBy{
              engine
                .onDataSet(dataset.id)
                .findElementDefinitionById(edId)
            }should have message expectedIdMsg
          }
        }

        it ("should FindElementDefinitionById"){
          val dataset = engine.findDataSetByName("Aliens")
          val xenomorph = engine.onDataSet(dataset.id).findElementDefinitionByName("Xenomorph")

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="ID",
            options=Map("dsId" -> dataset.id, "mid" -> xenomorph.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
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
          val dataset = engine.findDataSetByName("Aliens")
          val xenomorph = engine.onDataSet(dataset.id).findElementDefinitionByName("Xenomorph")

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="ElementDefinition",
            filter="Name",
            options=Map("dsId" -> dataset.id, "name" -> xenomorph.name)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
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
          val dsId = engine.createDataSet("Bands", "Interesting Music Groups")
          val edName = "RockBand"
          val edDesc = "A band that likes to roll..."
          val edId = engine.onDataSet(dsId)
            .defineElement(edName, edDesc)
            .withProperty("Name", "String", "The name of the band.")
            .withProperty("Singer", "String", "A person responsible for singing.")
            .withProperty("LeadGuitarist", "String", "The primary guitarists.")
          .end

          val request = buildWSRequest(user="Bob",
            actionType="Create",
            scope="DataSet",
            entityType="Element",
            filter="None",
            options=Map("dsId" -> dsId,
              "edId" -> edId,
              "Singer" -> "Axl Rose",
              "LeadGuitarist" -> "Slash",
              "Name" -> "Guns N' Roses"
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])

            payloadMap.contains("id") should equal(true)
            val eId = payloadMap("id").asInstanceOf[String]
            val element = engine.onDataSet(dsId).findElement(eId)
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
          val dataset = engine.findDataSetByName("Bands")
          val bands = engine.onDataSet(dataset.id).elements()
          val gnrOpt = bands.find{ prop =>
            prop.fields.contains("Name") &&
            prop.fields("Name") == "Guns N' Roses"
          }

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="Element",
            filter="ID",
            options=Map("dsId" -> dataset.id,
              "mid" -> gnrOpt.get.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("Elements") should equal(true)
            val elementsList = payloadMap("Elements").asInstanceOf[List[Map[String, Any]]]
            elementsList.length should equal(1)
          }
        }

        it("should FindAllElements"){
          val dataset = engine.findDataSetByName("Bands")
          val ed = engine.onDataSet(dataset.id).findElementDefinitionByName("RockBand")
          engine.onDataSet(dataset.id)
            .provision(ed.id)
            .withField("Name", "Daft Punk")
          .end

          val expectedElementsCount = engine.onDataSet(dataset.id).elements().length

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="Element",
            filter="All",
            options=Map("dsId" -> dataset.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            val elementsList = payloadMap("Elements").asInstanceOf[List[Map[String, Any]]]
            elementsList.length should equal(expectedElementsCount)
          }
        }

        it ("should EditElement"){
          val dsId = engine.createDataSet("Un-natural Disasters", "Epic Events of Calamity")
          val edId = engine.onDataSet(dsId)
            .defineElement("Disaster", "Oh No!")
            .withProperty("Name", "String", "The name of the disaster.")
          .end

          val zaId = engine.onDataSet(dsId).provision(edId).withField("Name", "Zombie Attack!").end()
          val maId = engine.onDataSet(dsId).provision(edId).withField("Name", "Monster Attack!").end()
          val zimID = engine.onDataSet(dsId).provision(edId).withField("Name", "Martians!").end()

          val updatedField = "Invader Zim Attack!"
          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="DataSet",
            entityType="Element",
            filter="None",
            options=Map("dsId" -> dsId,
              "elementId" -> zimID,
              "Name" -> updatedField)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zimID)

            //get the ID and verify it is zimID.
            //Using the engine to get the element and verify it has been updated.
            val martian = engine.onDataSet(dsId).findElement(zimID)
            martian.field[String]("Name") should equal(updatedField)
          }
        }

        it ("should AddElementField"){
          val dataset = engine.findDataSetByName("Un-natural Disasters")
          val elements = engine.onDataSet(dataset.id).elements()
          val zombieAttack = elements.find(e => e.field[String]("Name") == "Zombie Attack!").get
          zombieAttack.fields.size should equal(1)

          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="DataSet",
            entityType="Element",
            filter="None",
            options=Map("dsId" -> dataset.id,
              "elementId" -> zombieAttack.id,
              "DeadlyRanking" -> "4")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zombieAttack.id)
            val updatedElement = engine.onDataSet(dataset.id).findElement(zombieAttack.id)
            updatedElement.fields.size should equal(2)
          }
        }

        it ("should RemoveElementField"){
          val dataset = engine.findDataSetByName("Un-natural Disasters")
          val elements = engine.onDataSet(dataset.id).elements()
          val zombieAttack = elements.find(e => e.field[String]("Name") == "Zombie Attack!").get
          zombieAttack.fields.size should equal(2)

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="ElementField",
            filter="None",
            options=Map("dsId" -> dataset.id,
              "elementId" -> zombieAttack.id,
              "DeadlyRanking" -> "DeadlyRanking")
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zombieAttack.id)
            val updatedElement = engine.onDataSet(dataset.id).findElement(zombieAttack.id)
            updatedElement.fields.size should equal(1)
          }
        }

        it ("should DeleteElement"){
          val dataset = engine.findDataSetByName("Un-natural Disasters")
          val elements = engine.onDataSet(dataset.id).elements()
          elements.length should equal(3)
          val zombieAttack = elements.find(e => e.field[String]("Name") == "Zombie Attack!").get

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="Element",
            filter="None",
            options=Map("dsId" -> dataset.id,
              "elementId" -> zombieAttack.id)
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(zombieAttack.id)
            val updatedElementList = engine.onDataSet(dataset.id).elements
            updatedElementList.length should equal(2)
          }
        }

        it ("should AssociateElements"){
          val associationType = "friends_with"
          val request = buildWSRequest(user="Bob",
            actionType="Create",
            scope="DataSet",
            entityType="Association",
            filter="None",
            options=Map("dsId"    -> starwarsDsId,
              "startingElementId" -> hanId,
              "endingElementId"   -> chewieId,
              "associationName"   -> associationType,
              "friendshipType"    -> "best buds"
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])

            payloadMap.contains("id") should equal(true)
            val assocId           = payloadMap("id").toString
            val friendshipEternal = engine.onDataSet(starwarsDsId).findAssociation(assocId)

            friendshipEternal.id                should equal(assocId)
            friendshipEternal.associationType   should equal(associationType)
            friendshipEternal.startingElementId should equal(hanId)
            friendshipEternal.endingElementId   should equal(chewieId)
            friendshipEternal.fields.size       should equal(1)
          }
        }

        it ("should FindAssociationById"){
          val associations = engine.onDataSet(starwarsDsId).onElement(hanId).findOutboundAssociations()
          associations.length should equal(2)
          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="Association",
            filter="ID",
            options=Map(
              "associationId" -> friendsWith.id
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
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
          val associations = engine.onDataSet(starwarsDsId).onElement(hanId).findOutboundAssociations()
          associations.length should equal(2)
          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get

          val request = buildWSRequest(user="Bob",
            actionType="Update",
            scope="DataSet",
            entityType="Association",
            filter="None",
            options=Map(
              "associationId"  -> friendsWith.id,
              "friendshipType" -> "meaningful",
              "duration"       -> "A long time."
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(friendsWith.id)
            val updatedAssoc = engine.onDataSet(starwarsDsId).findAssociation(friendsWith.id)
            updatedAssoc.fields.size should equal(2)
            updatedAssoc.field[String]("friendshipType") should equal("meaningful")
            updatedAssoc.field[String]("duration") should equal("A long time.")
          }
        }

        it ("should RemoveAssociationField"){
          val associations = engine.onDataSet(starwarsDsId).onElement(hanId).findOutboundAssociations()
          associations.length should equal(2)
          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get
          friendsWith.fields.size should equal(2)

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="AssociationField",
            filter="None",
            options=Map(
              "associationId"  -> friendsWith.id,
              "duration"       -> "duration"
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(friendsWith.id)
            val updatedAssoc = engine.onDataSet(starwarsDsId).findAssociation(friendsWith.id)
            updatedAssoc.fields.size should equal(1)
            updatedAssoc.field[String]("friendshipType") should equal("meaningful")
          }
        }

        it ("should DeleteAssociation"){
          val associations = engine.onDataSet(starwarsDsId).onElement(hanId).findOutboundAssociations()
          associations.length should equal(2)
          val friendsWith = associations.find({a => a.associationType == "friends_with"}).get

          val request = buildWSRequest(user="Bob",
            actionType="Delete",
            scope="DataSet",
            entityType="Association",
            filter="None",
            options=Map(
              "associationId"  -> friendsWith.id
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            payloadMap("id") should equal(friendsWith.id)

            val expectedMsg = "No association with associationId: %s could be found.".format(friendsWith.id)
            the [InternalErrorException] thrownBy{
              engine.findAssociation(friendsWith.id)
            }should have message expectedMsg
          }
        }

        it ("should FindInboundAssociationsByElementId"){
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

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("Associations") should equal(true)
            val assocs = payloadMap("Associations").asInstanceOf[List[Map[String, Any]]]
            assocs.length should equal(1)
            assocs.head("associationType").toString should equal("married_to")
          }
        }

        it ("should FindOutboundAssociationsByElementId"){
          val associations = engine.onDataSet(starwarsDsId).onElement(leiaId).findInboundAssociations()
          associations.length should equal(1) //To Han

          val request = buildWSRequest(user="Bob",
            actionType="Retrieve",
            scope="DataSet",
            entityType="OutboundAssociation",
            filter="None",
            options=Map(
              "elementId"  -> leiaId
            )
          )

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = validateOkMsg(msgToMap(results.last))
            val payloadMap  = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("Associations") should equal(true)
            val assocs = payloadMap("Associations").asInstanceOf[List[Map[String, Any]]]
            assocs.length should equal(1)
            assocs.head("associationType").toString should equal("married_to")
          }
        }

        /*
        import org.machine.engine.viz.GraphVizHelper
        GraphVizHelper.visualize(engine.database)
        */
        ignore ("should RemoveInboundAssociations"){
          val associations = engine.onDataSet(starwarsDsId).onElement(hanId).findInboundAssociations()
          associations.length should equal(1)

        }

        it ("should RemoveOutboundAssociations")(pending)
        //Merge /w master

        it ("should FindDownStreamElementsByElementId")(pending)

        it ("should FindUpStreamElementsByElementId")(pending)
      }
    }
  }
}
