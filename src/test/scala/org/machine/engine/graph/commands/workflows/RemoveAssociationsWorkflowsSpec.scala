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
import org.machine.engine.graph.nodes.Association
import org.machine.engine.viz.GraphVizHelper

class RemoveAssociationsWorkflowsSpec extends FunSpecLike
  with Matchers
  with BeforeAndAfterAll{
  import RemoveAssociationsWorkflows._
  import TestUtils._
  import Neo4JHelper._

  private val config = ConfigFactory.load()
  private var engine:Engine = null
  private val options = GraphCommandOptions()
  private val emptyCapsule = (null, null, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))
  private var dsId:String = null
  private var edId:String = null
  private var hanId:String = null
  private var chewieId:String = null
  private var leiaId:String = null
  private var activeUserId:String = null

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    activeUserId = Engine.getInstance
      .createUser
      .withFirstName("Bob")
      .withLastName("Grey")
      .withEmailAddress("onebadclown@derry-maine.com")
      .withUserName("pennywise")
      .withUserPassword("You'll float too...")
    .end
    val sw = buildDataset()
    dsId = sw._1
    edId = sw._2
    hanId = sw._3
    chewieId = sw._4
    leiaId = sw._5
  }

  override def afterAll(){
    perge
  }

  def buildDataset():(String, String, String, String, String) = {
    val dsId = engine.forUser(activeUserId)
      .createDataSet("Star Wars", "Space Opera")
    val edId = engine.forUser(activeUserId)
      .onDataSet(dsId)
      .defineElement("Character", "A person in the movie.")
      .withProperty("Name", "String", "The name of the character.")
    .end

    val hanId = engine.forUser(activeUserId).onDataSet(dsId).provision(edId).withField("Name", "Han Solo").end
    val chewieId = engine.forUser(activeUserId).onDataSet(dsId).provision(edId).withField("Name", "Chewbacca").end
    val leiaId = engine.forUser(activeUserId).onDataSet(dsId).provision(edId).withField("Name", "Princess Leia Organa").end

    engine.forUser(activeUserId).inDataSet(dsId).attach(leiaId).to(hanId).as("married_to").withField("wears_the_pants", true).end
    engine.forUser(activeUserId).inDataSet(dsId).attach(hanId).to(leiaId).as("married_to").end
    engine.forUser(activeUserId).inDataSet(dsId).attach(hanId).to(chewieId).as("friends_with").end
    return (dsId, edId, hanId, chewieId, leiaId)
  }

  describe("Remove Associations Workflow Functions"){
    describe("Require Element ID"){
      it ("should not be defined when status is not OK"){
        val capsule = (null, null, options, mutable.Map.empty[String, Any], Right("error..."))
        requireElementId.isDefinedAt(capsule) should equal(false)
      }

      it ("should be defined when status is OK"){
        requireElementId.isDefinedAt(emptyCapsule) should equal(true)
      }

      it ("should set the status to an error when elementId is not an option"){
        options.reset
        requireElementId(emptyCapsule)._5 should equal(Right(ElementIdMissingErrorMsg))
      }

      it ("should set the status OK when elementId is an option"){
        options.reset
        options.addOption(ElementId, "123")
        requireElementId(emptyCapsule)._5 should equal(Left(WorkflowStatuses.OK))
      }
    }

    describe("Find Inbound Associations"){
      it ("should be defined when status is OK"){
        findInboundAssociations.isDefinedAt(emptyCapsule) should equal(true)
      }

      it ("should find inbound associations"){
        val capsule = (engine.database, CommandScopes.DataSetScope, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))

        options.reset
        options.addOption(ElementId, hanId)
        val processed = findInboundAssociations(capsule)
        processed._5 should equal(Left(WorkflowStatuses.OK))
        processed._4.contains(ExistingAssociations) should equal(true)
        val inboundAssociations = processed._4(ExistingAssociations).asInstanceOf[List[Association]]
        inboundAssociations.length should equal(1)
        inboundAssociations.head.fields.contains("wears_the_pants") should equal(true)
      }
    }

    describe("Find Outbound Associations"){
      it ("should be defined when status is OK"){
        findOutboundAssociations.isDefinedAt(emptyCapsule) should equal(true)
      }

      it ("should find outbound associations"){
        val capsule = (engine.database, CommandScopes.DataSetScope, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))

        options.reset
        options.addOption(ElementId, hanId)
        val processed = findOutboundAssociations(capsule)
        processed._5 should equal(Left(WorkflowStatuses.OK))
        processed._4.contains(ExistingAssociations) should equal(true)
        val outboundAssociations = processed._4(ExistingAssociations).asInstanceOf[List[Association]]
        outboundAssociations.length should equal(2)
      }
    }

    describe("Build Remove Inbound Associations Statement"){
      it ("should be defined when status is OK"){
        options.reset
        buildRemoveInboundAssociationsStmt.isDefinedAt(emptyCapsule) should equal(true)
      }

      it ("should build the removal statement"){
        options.reset
        val pretext = mutable.Map.empty[String, Any]
        pretext += (ExistingAssociations -> List(
          Association("123", null, null, null, null, null, null),
          Association("456", null, null, null, null, null, null)))
        val capsule = (null, null, options, pretext, Left(WorkflowStatuses.OK))

        val (db, scope, opts, context, status) = buildRemoveInboundAssociationsStmt(capsule)

        status should equal(Left(WorkflowStatuses.OK))
        context.contains(RemoveAssociationsStmt) should equal(true)
      }
    }

    describe("Build Remove Outbound Associations Statement"){
      it ("should be defined when status is OK"){
        buildRemoveOutboundAssociationsStmt.isDefinedAt(emptyCapsule) should equal(true)
      }

      it ("should build the removal statement"){
        options.reset
        val pretext = mutable.Map.empty[String, Any]
        pretext += (ExistingAssociations -> List(
          Association("123", null, null, null, null, null, null),
          Association("456", null, null, null, null, null, null)))
        val capsule = (null, null, options, pretext, Left(WorkflowStatuses.OK))
        val (db, scope, opts, context, status) = buildRemoveOutboundAssociationsStmt(capsule)
        status should equal(Left(WorkflowStatuses.OK))
        context.contains(RemoveAssociationsStmt) should equal(true)
      }
    }

    describe("Remove Associations"){
      it ("should be defined when status is OK"){
        removeAssociations.isDefinedAt(emptyCapsule) should equal(true)
      }

      it ("should change the status to an error if RemoveAssociationsStmt in not in the context"){
        val capsule = (null, null, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))
        capsule._4 += (AssociationIds -> Seq.empty[String])
        removeAssociations(capsule)._5 should equal(Right(RemoveAssociationsStmtRequiredErrorMsg))
      }

      it ("should remove inbound associations"){
        engine.inDataSet(dsId).onElement(hanId).findInboundAssociations().length should equal(1)
        engine.inDataSet(dsId).onElement(hanId).findOutboundAssociations().length should equal(2)

        options.reset
        options.addOption(ElementId, hanId)
        val capsule = (engine.database, CommandScopes.DataSetScope, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))
        val (db, scope, opts, context, status) = removeInboundAssociations(capsule)
        status should equal(Left(WorkflowStatuses.OK))
        val inAssoc = engine.inDataSet(dsId).onElement(hanId).findInboundAssociations()
        val outAssoc = engine.inDataSet(dsId).onElement(hanId).findOutboundAssociations()
        inAssoc.length should equal(0)
        outAssoc.length should equal(2)
      }

      it ("should remove outbound associations"){
        engine.inDataSet(dsId).onElement(hanId).findInboundAssociations().length should equal(0)
        engine.inDataSet(dsId).onElement(hanId).findOutboundAssociations().length should equal(2)

        options.reset
        options.addOption(ElementId, hanId)
        val capsule = (engine.database, CommandScopes.DataSetScope, options, mutable.Map.empty[String, Any], Left(WorkflowStatuses.OK))
        val (db, scope, opts, context, status) = removeOutboundAssociations(capsule)
        status should equal(Left(WorkflowStatuses.OK))
        val inAssoc = engine.inDataSet(dsId).onElement(hanId).findInboundAssociations()
        val outAssoc = engine.inDataSet(dsId).onElement(hanId).findOutboundAssociations()
        inAssoc.length should equal(0)
        outAssoc.length should equal(0)
      }
    }
  }
}
