package org.machine.engine.graph

import org.scalatest._
import org.scalatest.mock._

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

//For iterating over Java Collections
import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}

import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.graph._
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._


class ElementAssociationsSpec extends FunSpec
  with Matchers
  with EasyMockSugar
  with BeforeAndAfterAll{
  import Neo4JHelper._
  import TestUtils._
  private var engine:Engine = null
  private var systemsDataSetId:String = null
  private var noteElementDefininitionId:String = null
  private var systemElementDefinitionId:String = null
  private var systemId:String = null
  private var noteId:String = null
  private var workerElementDefinitionId:String = null
  private var bizCapElementDefId:String = null
  private var workerId:String = null
  private var erpId:String = null
  private var bizCapId:String = null
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
    systemsDataSetId = engine.forUser(activeUserId).createDataSet("System Under Review", "System that need to be reviewed.")
    noteElementDefininitionId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .defineElement("note", "short piece of text")
        .withProperty("title", "String", "The title of the note.")
        .withProperty("description", "String", "An optional description of the note.")
        .withProperty("body", "String", "The body of the note.")
    .end

    systemElementDefinitionId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .defineElement("system", "A set of interacting or interdependent components forming an integrated whole.")
        .withProperty("name", "String", "The name of the system.")
        .withProperty("description", "String", "An optional description of the system.")
    .end

    workerElementDefinitionId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .defineElement("worker", "An employee or contractor who works for a company.")
        .withProperty("first_name", "String", "The primary name of the person.")
        .withProperty("last_name", "String", "The family name of the person.")
    .end

    bizCapElementDefId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .defineElement("business_capability", "The expression or the articulation of the capacity, materials and expertise an organization needs in order to perform core functions.")
      .withProperty("name", "String", "The name of the capability.")
      .withProperty("description", "String", "The description of the capability.")
    .end

    systemId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .provision(systemElementDefinitionId)
        .withField("name", "Publishing System")
        .withField("description", "Publishes stuff.")
    .end

    erpId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .provision(systemElementDefinitionId)
        .withField("name", "ERP System")
        .withField("description", "Manages OTC, Accounts...")
    .end

    noteId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .provision(noteElementDefininitionId)
        .withField("title", "Quick Note")
        .withField("body", "Talk to Chuck about what is being published.")
    .end

    workerId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .provision(workerElementDefinitionId)
      .withField("first_name", "Jon")
      .withField("last_name", "Snow")
    .end

    bizCapId = engine.forUser(activeUserId)
      .onDataSet(systemsDataSetId)
      .provision(bizCapElementDefId)
      .withField("name", "Publishing")
      .withField("description", "Publishing things for people.")
    .end
  }

  override def afterAll(){
    perge
  }

  describe("Machine Engine"){
    describe("DataSet"){
      describe("Element Associations"){
        it("should associate two elements"){
          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "A. Sterling")
          .end

          val annotation = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .findAssociation(annotationId)

          annotation should have(
            'id (annotationId),
            'associationType ("annotates"),
            'startingElementId (noteId),
            'endingElementId (systemId)
          )

          annotation.fields should have size 1
          annotation.field[String]("createdBy") should equal("A. Sterling")
        }

        it ("should associate without specifying as()"){
          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .withField("createdBy", "A. Sterling")
          .end

          val annotation = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .findAssociation(annotationId)

          annotation should have(
            'id (annotationId),
            'associationType ("is_associated_with")
          )

          annotation.fields should have size 1
          annotation.field[String]("createdBy") should equal("A. Sterling")
        }

        it("should update properties on an association"){
          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "A. Sterling")
          .end

          engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onAssociation(annotationId)
            .setField("createdBy", "Pam")
          .end

          val association = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .findAssociation(annotationId)

          association.field[String]("createdBy") should equal("Pam")
        }

        it("should remove an association between two elements"){
          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "Cherl")
          .end

          engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onAssociation(annotationId)
            .delete
          .end

          val expectedMsg = "No association with associationId: %s could be found.".format(annotationId)
          the [InternalErrorException] thrownBy{
            engine.forUser(activeUserId)
              .inUserSpace
              .findAssociation(annotationId)
          }should have message expectedMsg
        }

        it ("should remove a property on a relationship"){
          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "Neo")
            .withField("meta", "There is no spoon.")
            .withField("amazement", "Wow...")
          .end

          engine.forUser(activeUserId)
            .inUserSpace
            .findAssociation(annotationId)
            .fields should have size 3

          engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onAssociation(annotationId)
            .removeField("meta")
          .end

          engine.forUser(activeUserId)
            .inUserSpace
            .findAssociation(annotationId)
            .fields should have size 2

          engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onAssociation(annotationId)
            .removeField("createdBy")
            .removeField("amazement")
          .end

          engine.forUser(activeUserId)
            .inUserSpace
            .findAssociation(annotationId)
            .fields should have size 0
        }

        it("should find outbound associations of an element"){
          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "J. Snow")
          .end

          val contactId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(workerId)
            .to(systemId)
            .as("is_a_contact_for")
            .withField("primaryContact", true)
          .end

          val capId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(systemId)
            .to(bizCapId)
            .as("enables")
            .withField("identifiedBy", "Jon Snow")
          .end

          val integrationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(systemId)
            .to(erpId)
            .as("is_integrated_with")
            .withField("note", "Existing integration shall be replaced next year.")
          .end

          val outBoundAssociations:Seq[Association] = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onElement(systemId)
            .findOutboundAssociations()

          outBoundAssociations should have length 2
          outBoundAssociations(0) should have(
            'id (capId),
            'associationType ("enables"),
            'startingElementId (systemId),
            'endingElementId (bizCapId)
          )

          outBoundAssociations(0).field[String]("identifiedBy") should equal("Jon Snow")

          outBoundAssociations(1) should have(
            'id (integrationId),
            'associationType ("is_integrated_with"),
            'startingElementId (systemId),
            'endingElementId (erpId)
          )

          outBoundAssociations(1).field[String]("note") should equal("Existing integration shall be replaced next year.")
        }

        it("should find inbound associations of an element"){
          //need to clear the existing connections or create a new node.
          engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onElement(systemId)
            .removeInboundAssociations

          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "J. Snow")
          .end

          val contactId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(workerId)
            .to(systemId)
            .as("is_a_contact_for")
            .withField("primaryContact", true)
          .end

          val inboundAssociations:Seq[Association] = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onElement(systemId)
            .findInboundAssociations()

          inboundAssociations should have length 2

          inboundAssociations(0) should have(
            'id (annotationId),
            'associationType ("annotates"),
            'startingElementId (noteId),
            'endingElementId (systemId)
          )

          inboundAssociations(1) should have(
            'id (contactId),
            'associationType ("is_a_contact_for"),
            'startingElementId (workerId),
            'endingElementId (systemId)
          )
        }

        it("should find upstream (parents) elements"){
          engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onElement(systemId)
            .removeInboundAssociations

          val annotationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "J. Snow")
          .end

          val contactId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(workerId)
            .to(systemId)
            .as("is_a_contact_for")
            .withField("primaryContact", true)
          .end

          val upstreamElements:Seq[Element] = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onElement(systemId)
            .findUpStreamElements

          upstreamElements should have length 2    

          upstreamElements(0) should have(
            'id (noteId)
          )

          upstreamElements(1) should have(
            'id (workerId)
          )
        }

        it("should find downstream (children) elements"){
          engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onElement(systemId)
            .removeOutboundAssociations

          val capId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(systemId)
            .to(bizCapId)
            .as("enables")
            .withField("identifiedBy", "Jon Snow")
          .end

          val integrationId = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .attach(systemId)
            .to(erpId)
            .as("is_integrated_with")
            .withField("note", "Existing integration shall be replaced next year.")
          .end

          val downstreamElements:Seq[Element] = engine.forUser(activeUserId)
            .onDataSet(systemsDataSetId)
            .onElement(systemId)
            .findDownStreamElements

          downstreamElements should have length 2

          downstreamElements(0) should have(
            'id (bizCapId)
          )

          downstreamElements(1) should have(
            'id (erpId)
          )
        }
      }
    }
  }
}
