package org.machine.engine.graph.commands.workflows

import org.scalatest._
import org.scalatest.mock._
import org.machine.engine.TestUtils

import java.io.File;
import java.io.IOException;
import org.neo4j.io.fs.FileUtils

import com.typesafe.config._
import scala.collection.mutable
import scala.util.{Either, Left, Right}

import org.neo4j.graphdb.GraphDatabaseService
import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.{CommandScope, CommandScopes, GraphCommandOptions}
import org.machine.engine.graph.nodes.{Element, PropertyDefinition, PropertyDefinitions}
import org.machine.engine.viz.GraphVizHelper

class ListAllElementsWorkflowSpec extends FunSpecLike
  with Matchers  with BeforeAndAfterAll{
  import ListAllElementsWorkflow._
  import TestUtils._
  import Neo4JHelper._

  private val config = ConfigFactory.load()
  var engine:Engine = null
  val options = GraphCommandOptions()

  override def beforeAll(){
    engine = Engine.getInstance
    perge
  }

  override def afterAll(){
    perge
  }

  describe("List Element Workflow Functions"){
    describe("Verify Required Command Options"){
      it ("should be defined when status is OK"){
        val capsule = (null, null, null, null, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions.isDefinedAt(capsule) should equal(true)
      }

      it ("should not be defined if status is not OK"){
        val errorCapsule = (null, null, null, null, Left(WorkflowStatuses.Error))
        verifyRequiredCmdOptions.isDefinedAt(errorCapsule) should equal(false)

        val exceptionCapsule = (null, null, null, null, Right("An exception message..."))
        verifyRequiredCmdOptions.isDefinedAt(exceptionCapsule) should equal(false)
      }

      it ("should be OK if dataset ID is present in the options"){
        options.reset
        options.addOption(DataSetId, "123")
        val capsule = (null, null, options, null, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._5 should equal(Left(WorkflowStatuses.OK))
      }

      it ("should be OK if dataset NAME is present in the options"){
        options.reset
        options.addOption(DataSetName, "Blah")
        val capsule = (null, null, options, null, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._5 should equal(Left(WorkflowStatuses.OK))
      }

      it ("should return an error if no dataset identifiers are provided in the options"){
        options.reset
        val capsule = (null, null, options, null, Left(WorkflowStatuses.OK))
        verifyRequiredCmdOptions(capsule)._5 should equal(Right(DataSetFilterRequiredErrorMsg))
      }
    }

    describe("Generate Query"){
      it ("should not be defined when scope is not dataset"){
        val capsule = (null, null, null, null, Left(WorkflowStatuses.OK))
        generateQuery.isDefinedAt(capsule) should equal(false)
      }

      it ("should not be defined when status is not OK"){
        val capsule = (null, CommandScopes.DataSetScope, null, null, Right("an error..."))
        generateQuery.isDefinedAt(capsule) should equal(false)
      }

      it ("should be defined when status is OK & scope is dataset"){
        val capsule = (null, CommandScopes.DataSetScope, null, null, Left(WorkflowStatuses.OK))
        generateQuery.isDefinedAt(capsule) should equal(true)
      }

      it ("should generate a query with dataset ID"){
        options.reset
        options.addOption(DataSetId, "123")
        val capsule = (null, CommandScopes.DataSetScope, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))
        val processed = generateQuery(capsule)
        processed._5 should equal(Left(WorkflowStatuses.OK))
        processed._4.contains(FindElementsQuery) should equal(true)
      }

      it ("should generate a query with dataset NAME"){
        options.reset
        options.addOption(DataSetName, "Blah")
        val capsule = (null, CommandScopes.DataSetScope, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))
        val processed = generateQuery(capsule)
        processed._5 should equal(Left(WorkflowStatuses.OK))
        processed._4.contains(FindElementsQuery) should equal(true)
      }

      it ("should return an error if no dataset identifiers are provided in the options"){
        {
          options.reset
          val capsule = (null, CommandScopes.DataSetScope, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))
          val processed = generateQuery(capsule)
          processed._5 should equal(Right(DataSetFilterRequiredErrorMsg))
          processed._4.contains(FindElementsQuery) should equal(false)
        }
      }
    }

    describe("Find Elements"){
      it ("should not be defined when status is not OK"){
        val context = mutable.Map.empty[String, Any]
        context += (FindElementsQuery -> "A query...")
        val capsule = (null, null, null, context, Right("error msg..."))
        findElements.isDefinedAt(capsule) should equal(false)
      }

      it ("should not be defined when query is not in the context"){
        val context = mutable.Map.empty[String, Any]
        val capsule = (null, null, null, context, Left(WorkflowStatuses.OK))
        findElements.isDefinedAt(capsule) should equal(false)
      }

      it ("should be defined when status is OK & query in in context"){
        val context = mutable.Map.empty[String, Any]
        context += (FindElementsQuery -> "A query...")
        val capsule = (null, null, null, context, Left(WorkflowStatuses.OK))
        findElements.isDefinedAt(capsule) should equal(true)
      }

      it ("should find all elements in a dataset"){
        val dsId = engine.createDataSet("Moody Play List", "Songs for when I'm feeling dark.")
        val edId = engine.onDataSet(dsId)
          .defineElement("Song", "Kicking Beats")
          .withProperty("Title", "String", "Name of the song.")
          .withProperty("Artist", "String", "Name of the musician or band.")
        .end

        val trackAId = engine.onDataSet(dsId)
          .provision(edId)
          .withField("Title", "Far From Any Road")
          .withField("Artist", "The Handsome Family")
        .end

        val trackBId = engine.onDataSet(dsId)
          .provision(edId)
          .withField("Title", "Red Right Hand")
          .withField("Artist", "Nick Cave")
        .end

        val trackCId = engine.onDataSet(dsId)
          .provision(edId)
          .withField("Title", "Doubting Thomas")
          .withField("Artist", "Nickel Creek")
        .end

        options.reset
        options.addOption(DataSetId, dsId)
        val capsule = (engine.database,
          CommandScopes.DataSetScope,
          options,
          mutable.Map.empty[String, Any],
          Left(WorkflowStatuses.OK))

        val processed = workflow(capsule)
        processed._5 should equal(Left(WorkflowStatuses.OK))
        processed._4.contains(FoundElements) should equal(true)
        val elements:List[Element] = processed._4(FoundElements).asInstanceOf[List[Element]]
        elements.length should equal(3)
      }
    }
  }
}
