package org.machine.engine.graph.commands.elementdefinition

import org.scalatest._
import org.scalatest.mock._
import org.machine.engine.TestUtils

import java.io.File;
import java.io.IOException;
import org.neo4j.io.fs.FileUtils

import com.typesafe.config._
import org.neo4j.graphdb.GraphDatabaseService
import org.machine.engine.Engine
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.{CommandScope, CommandScopes, GraphCommandOptions}
import org.machine.engine.graph.nodes.{PropertyDefinition, PropertyDefinitions}
import scala.util.{Either, Left, Right}
import org.machine.engine.viz.GraphVizHelper

class ElementDefintionWorkflowFunctionsSpec extends FunSpecLike
  with Matchers  with BeforeAndAfterAll{
  import ElementDefintionWorkflowFunctions._
  import TestUtils._
  import Neo4JHelper._

  private val config = ConfigFactory.load()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null
  val options = GraphCommandOptions()

  override def beforeAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
    engine = Engine.getInstance
  }

  override def afterAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Element Defintion Workflow Functions"){
    describe("generate mid if not present"){
      it("should be defined at when workflow status is ok and options does not contains mid"){
        options.reset
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        mIdGuard.isDefinedAt(capsule) should be(true)
      }

      it("should not be defined at when workflow status is error"){
        options.reset
        val capsule = (null, null, options, Left(WorkflowStatuses.Error))
        mIdGuard.isDefinedAt(capsule) should be(false)
      }

      it("should not be defined at with mid is provided"){
        options.reset
        options.addOption("mid","123")
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        mIdGuard.isDefinedAt(capsule) should be(false)
      }

      it("should apply mid when none exists"){
        options.reset
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        mIdGuard(capsule)
        options.option[String]("mid") should not be null
      }
    }

    describe("verifying required command options"){
      it("should be defined at when workflow is ok"){
        val capsule = (null, null, null, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions.isDefinedAt(capsule) should equal(true)
      }

      it("should require mid"){
        options.reset
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._4 should equal(Right(MissingMidErrorMsg))
      }

      it("should require name"){
        options.reset
        options.addOption("mid","123")
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._4 should equal(Right(MissingNameErrorMsg))
      }

      it("should require description"){
        options.reset
        options.addOption("mid","123")
        options.addOption("name","abc")
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._4 should equal(Right(MissingDescErrorMsg))
      }

      it("should require creationTime"){
        options.reset
        options.addOption("mid","123")
        options.addOption("name","abc")
        options.addOption("description","asdfas")
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._4 should equal(Right(MissingCreationTimeErrorMsg))
      }

      it ("should set the status to OK when all required options are provided"){
        options.reset
        options.addOption("mid","123")
        options.addOption("name","abc")
        options.addOption("description","asdfas")
        options.addOption("creationTime", Neo4JHelper.time)
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._4 should equal(Left(WorkflowStatuses.OK))
      }
    }

    describe("verifing uniqueness"){
      it("should be defined at when workflow is ok"){
        val capsule = (null, null, null, Left(WorkflowStatuses.OK))
        verifyUniqueness.isDefinedAt(capsule) should equal(true)
      }

      it("should not be defined at when workflow is error"){
        val capsule = (null, null, null, Right("error msg"))
        verifyUniqueness.isDefinedAt(capsule) should equal(false)
      }

      /*
      If we where trying to create an element definition with name "Gem" in a Dataset.
      */
      it("should pass status of OK when no element definition exists in scope of dataset"){
        options.reset
        val dsId = engine.createDataSet("Dataset A", "A dataset")
        options.addOption("dsId", dsId)
        options.addOption("name", "Gem") //The Element Definitions name...
        val capsule = (engine.database, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = verifyUniqueness(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
      }

      it("should pass error message when an element definition exists in scope of dataset"){
        options.reset
        val dsId = engine.createDataSet("Dataset B", "Another dataset")
        engine
          .onDataSet(dsId)
          .defineElement("Gem", "A precious stone.")
          .withProperty("color", "String", "The color of light the gem reflects.")
        .end

        options.addOption("dsId", dsId)
        options.addOption("name", "Gem")

        val capsule = (engine.database, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = verifyUniqueness(capsule)
        processed._4 should equal(Right("Element Definition already exists with the provided name."))
      }
    }

    describe("Create Element Definition Statement"){
      it ("should be defined at when status is Left(OK)"){
        val capsule = (null, null, null, Left(WorkflowStatuses.OK))
        createElementDefinitionStmt.isDefinedAt(capsule) should equal(true)
      }
      it ("should not be defined at when status is Right(String)"){
        val capsule = (null, null, null, Right("error msg"))
        createElementDefinitionStmt.isDefinedAt(capsule) should equal(false)
      }

      it ("should generate a create statement with dsId"){
        options.reset
        options.addOption("dsId", "123")
        val capsule = (null, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = createElementDefinitionStmt(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createElementDefinitionStmt") should equal(true)
        val expected = normalize("""
        |match (ss:internal_data_set) where ss.mid = {dsId}
        |create (ss)-[:exists_in]->(ed:element_definition {
        |  mid:{mid},
        |  name:{name},
        |  description:{description},
        |  creation_time:{creationTime}
        |})
        |return ed.mid as edId
        """.stripMargin)
        val actual = normalize(processed._3.option[String]("createElementDefinitionStmt"))
        actual should equal(expected)
      }

      it ("should generate a create statement with dsName"){
        options.reset
        options.addOption("dsName", "asdf")
        val capsule = (null, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = createElementDefinitionStmt(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createElementDefinitionStmt") should equal(true)
        val expected = normalize("""
        |match (ss:internal_data_set) where ss.name = {dsName}
        |create (ss)-[:exists_in]->(ed:element_definition {
        |  mid:{mid},
        |  name:{name},
        |  description:{description},
        |  creation_time:{creationTime}
        |})
        |return ed.mid as edId
        """.stripMargin)
        val actual = normalize(processed._3.option[String]("createElementDefinitionStmt"))
        actual should equal(expected)
      }

      it ("should generate a create statement in user space scope"){
        options.reset
        val capsule = (null, CommandScopes.UserSpaceScope, options, Left(WorkflowStatuses.OK))
        val processed = createElementDefinitionStmt(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createElementDefinitionStmt") should equal(true)
        val expected = normalize("""
        |match (ss:internal_user_space)
        |create (ss)-[:exists_in]->(ed:element_definition {
        |  mid:{mid},
        |  name:{name},
        |  description:{description},
        |  creation_time:{creationTime}
        |})
        |return ed.mid as edId
        """.stripMargin)
        val actual = normalize(processed._3.option[String]("createElementDefinitionStmt"))
        actual should equal(expected)
      }

      it ("should generate a create statement in system space scope"){
        options.reset
        val capsule = (null, CommandScopes.SystemSpaceScope, options, Left(WorkflowStatuses.OK))
        val processed = createElementDefinitionStmt(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createElementDefinitionStmt") should equal(true)
        val expected = normalize("""
        |match (ss:internal_system_space)
        |create (ss)-[:exists_in]->(ed:element_definition {
        |  mid:{mid},
        |  name:{name},
        |  description:{description},
        |  creation_time:{creationTime}
        |})
        |return ed.mid as edId
        """.stripMargin)
        val actual = normalize(processed._3.option[String]("createElementDefinitionStmt"))
        actual should equal(expected)
      }

      it ("should throw an exception when dataset scope doesn't provide dsId or dsName"){
        options.reset
        val capsule = (null, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = createElementDefinitionStmt(capsule)
        processed._4 should equal(Right(DataSetFilterRequiredErrorMsg))
      }
    }

    describe("Create Element Definition"){
      it ("should be defined at when status is Left(OK) and options contains createElementDefinitionStmt"){
        options.reset
        options.addOption("createElementDefinitionStmt", "abc")
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        createElementDefinition.isDefinedAt(capsule) should equal(true)
      }

      it ("should not be defined at when status is Left(OK) and options does not contain createElementDefinitionStmt"){
        options.reset
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        createElementDefinition.isDefinedAt(capsule) should equal(false)
      }

      it ("should not be defined at when status is Right(String)"){
        val capsule = (null, null, options, Right("error msg"))
        createElementDefinition.isDefinedAt(capsule) should equal(false)
      }

      val minimalCreateWF = Function.chain(Seq(createElementDefinitionStmt, createElementDefinition))
      it ("should create the new element definition node and associate it with a dataset by dsId"){
        options.reset
        val dsId = engine.createDataSet("Dataset C", "Yet Another dataset")
        options.addOption("dsId", dsId)
        val edId = uuid()
        options.addOption("mid", edId)
        options.addOption("name", "box")
        options.addOption("description", "A container with equal sized dimensions on all axis.")
        options.addOption("creationTime", time)

        val capsule = (engine.database, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = minimalCreateWF(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createdElementDefinitionId") should equal(true)

        val mappedEdIds = findEdOnDSById(dsId, edId)
        mappedEdIds.length should equal(1)
        mappedEdIds.head should equal(edId)
      }

      it ("should create the new element definition node and associate it with a dataset by dsName"){
        options.reset
        val DsName = "Dataset D"
        val dsId = engine.createDataSet(DsName, "Another dataset")
        options.addOption("dsName", DsName)
        val edId = uuid()
        options.addOption("mid", edId)
        options.addOption("name", "box")
        options.addOption("description", "A container with equal sized dimensions on all axis.")
        options.addOption("creationTime", time)

        val capsule = (engine.database, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = minimalCreateWF(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createdElementDefinitionId") should equal(true)

        val mappedEdIds = findEdOnDSById(dsId, edId)
        mappedEdIds.length should equal(1)
        mappedEdIds.head should equal(edId)
      }

      it ("should create the new element definition node and associate it with a user"){
        options.reset
        val edId = uuid()
        options.addOption("mid", edId)
        options.addOption("name", "box")
        options.addOption("description", "A container with equal sized dimensions on all axis.")
        options.addOption("creationTime", time)

        val capsule = (engine.database, CommandScopes.UserSpaceScope, options, Left(WorkflowStatuses.OK))
        val processed = minimalCreateWF(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createdElementDefinitionId") should equal(true)

        val mappedEdIds = findEdInUsById(edId)
        mappedEdIds.length should equal(1)
        mappedEdIds.head should equal(edId)
      }

      it ("should create the new element definition node and associate it with the system space"){
        options.reset
        val edId = uuid()
        options.addOption("mid", edId)
        options.addOption("name", "box")
        options.addOption("description", "A container with equal sized dimensions on all axis.")
        options.addOption("creationTime", time)

        val capsule = (engine.database, CommandScopes.SystemSpaceScope, options, Left(WorkflowStatuses.OK))
        val processed = minimalCreateWF(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        processed._3.contains("createdElementDefinitionId") should equal(true)

        val mappedEdIds = findEdInSsById(edId)
        mappedEdIds.length should equal(1)
        mappedEdIds.head should equal(edId)
      }
    }

    val minimalCreateWithPropsWF = Function.chain(Seq(createElementDefinitionStmt, createElementDefinition, createPropertyDefinitions))

    describe("Create Property Definitions"){
      it ("should be defined at when status is Left(OK), options contains createdElementDefinitionId & properties"){
        options.reset
        options.addOption("createdElementDefinitionId", "123")
        val props = new PropertyDefinitions()
        props.addProperty(PropertyDefinition(uuid, "pA", "String", "A property"))
        options.addOption("properties", props)
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        createPropertyDefinitions.isDefinedAt(capsule) should equal(true)
      }

      it ("should not be defined at when properties are empty"){
        options.reset
        options.addOption("createdElementDefinitionId", "123")
        val props = new PropertyDefinitions()
        options.addOption("properties", props)
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        createPropertyDefinitions.isDefinedAt(capsule) should equal(false)
      }

      it ("should not be defined at when properties are not provided"){
        options.reset
        options.addOption("createdElementDefinitionId", "123")
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        createPropertyDefinitions.isDefinedAt(capsule) should equal(false)
      }

      it ("should not be defined at when createdElementDefinitionId not provided"){
        options.reset
        val props = new PropertyDefinitions()
        props.addProperty(PropertyDefinition(uuid, "pA", "String", "A property"))
        options.addOption("properties", props)
        val capsule = (null, null, options, Left(WorkflowStatuses.OK))
        createPropertyDefinitions.isDefinedAt(capsule) should equal(false)
      }

      it ("should not be defined at when status is Right(msg)"){
        options.reset
        options.addOption("createdElementDefinitionId", "123")
        val props = new PropertyDefinitions()
        props.addProperty(PropertyDefinition(uuid, "pA", "String", "A property"))
        options.addOption("properties", props)
        val capsule = (null, null, options, Right("woops..."))
        createPropertyDefinitions.isDefinedAt(capsule) should equal(false)
      }

      /*
      Next Steps
      1. This demonstrates a bug. There are two EDs in system space with the same name.
      2. Add verifyUniqueness to the workflow and make sure that it won't
         allow two EDs with the same name in system space.
      3. Add more asserts on this test. Find the created props on the ed.
      */
      it("should enforce unique element definitions in system space"){
        options.reset

        val edId = uuid()
        options.addOption("mid", edId)
        options.addOption("name", "box")
        options.addOption("description", "A container with equal sized dimensions on all axis.")
        options.addOption("creationTime", time)

        val props = new PropertyDefinitions().addProperty(PropertyDefinition(uuid, "pA", "String", "A property"))
        options.addOption("properties", props)

        val capsule = (engine.database, CommandScopes.SystemSpaceScope, options, Left(WorkflowStatuses.OK))
        val processed = workflow(capsule)
        processed._4 should equal(Right(ElementDefAlreadyExistsErrorMsg))
      }

      /*
      I believe this demonstrates a bug.
      Properties the ED & Props are not being created.
      */
      it("should create properties on element defintion in a dataset by dsName"){
        options.reset
        val dsName = "Dataset F"
        val dsId = engine.createDataSet(dsName, "Dataset. My Dataset. Oh how I've missed you.")
        options.addOption("dsName", dsName)

        val edId = uuid()
        options.addOption("mid", edId)
        options.addOption("name", "box")
        options.addOption("description", "A container with equal sized dimensions on all axis.")
        options.addOption("creationTime", time)

        val props = new PropertyDefinitions().addProperty(PropertyDefinition(uuid, "pA", "String", "A property"))
        options.addOption("properties", props)
        val capsule = (engine.database, CommandScopes.DataSetScope, options, Left(WorkflowStatuses.OK))
        val processed = minimalCreateWithPropsWF(capsule)
        processed._4 should equal(Left(WorkflowStatuses.OK))
        GraphVizHelper.visualize(engine.database)

        //Use FindElementDefinitionById & verify the props are indeed associated with the ed.
      }

      it("should create properties on element defintion in a dataset by dsId")(pending)
      it("should create properties on element defintion in user space")(pending)
      it("should create properties on element defintion in system space")(pending)
    }
  }

  def findEdOnDSById(dsId: String, edId: String):Seq[String] = {
    val stmt = """
    |match (ds:internal_data_set {mid:{dsId}})-[:exists_in]->(ed:element_definition {mid:{mid}})
    |return ed.mid as edId
    """.stripMargin

    val validationOptions = GraphCommandOptions().addOption("dsId", dsId)
      .addOption("mid", edId)

    val mappedEdIds:Array[String] = query[String](engine.database,
      stmt,
      validationOptions.toJavaMap,
      elementDefIdResultsProcessor)
    return mappedEdIds.toList
  }

  def findEdInUsById(edId: String):Seq[String] = {
    val stmt = """
    |match (ds:internal_user_space)-[:exists_in]->(ed:element_definition {mid:{mid}})
    |return ed.mid as edId
    """.stripMargin

    val validationOptions = GraphCommandOptions().addOption("mid", edId)

    val mappedEdIds:Array[String] = query[String](engine.database,
      stmt,
      validationOptions.toJavaMap,
      elementDefIdResultsProcessor)
    return mappedEdIds.toList
  }

  def findEdInSsById(edId: String):Seq[String] = {
    val stmt = """
    |match (ds:internal_system_space)-[:exists_in]->(ed:element_definition {mid:{mid}})
    |return ed.mid as edId
    """.stripMargin

    val validationOptions = GraphCommandOptions().addOption("mid", edId)

    val mappedEdIds:Array[String] = query[String](engine.database,
      stmt,
      validationOptions.toJavaMap,
      elementDefIdResultsProcessor)
    return mappedEdIds.toList
  }

}
