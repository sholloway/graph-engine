package org.machine.engine.graph

import org.machine.engine.graph.nodes._

/*
TODO: Change where Command scope lives. Should not be in org.machine.engine.graph.commands.
*/
import org.machine.engine.graph.commands.{CommandScope, EngineCmdResult, GraphCommandOptions}
import org.machine.engine.graph.decisions._

/** Definition of internal DSL for working with the underling graph database.
* ==Graph DSL==
* The graph DSL is an internal domain specific language designed for making
* it easier to use Neo4J internally to the engine.
*
* ==Concepts==
* Conceptually, the DSL is broken into spaces. System Space, User Space and Datasets.
* System space is designated for internal component definition. User space is
* intended for user component definition. Datasets are graphs created in the
* scope of the user space.
*
* The graph is composed of ElementDefinitions, Elements and Associations. An ElementDefinition
* is a definition of an element that can be provisioned. An Element is an instance
* of an ElementDefinition and a node in the graph. Associations are the edges between the Element nodes.
* Data can be attached and managed on both Elements and Associations.
*
* ==System Space Usage==
* '''Defining an ElementDefinition'''
* {{{
*   engine
*     .inSystemSpace()
*     .defineElement("Note", "A brief record of something, captured to assist the memory or for future reference.")
*     .withProperty("Note Text", "String", "The body of the note.")
*     .withProperty("Title", "String", "A user defined title of the note.")
*   .end
* }}}
*
* '''List all ElementDefintions'''
* {{{
* val elements:List[ElementDefinition] =
*   engine
*     .inSystemSpace()
*     .elementDefinitions()
* }}}
*
* '''Find an ElementDefintion by ID'''
* {{{
*  engine
*    .inSystemSpace()
*    .findElementDefinitionById(id)
* }}}
*
* '''Update an ElementDefintion'''
* {{{
* engine
*   .inSystemSpace()
*   .onElementDefinition(systemOption.get.id)
*   .setName(updatedName)
*   .setDescription(updatedDescription)
* .end
* }}}
*
* '''Update an ElementDefintion's PropertyDefinition'''
* {{{
* engine
*   .inSystemSpace()
*   .onElementDefinition(id)
*   .editPropertyDefinition(propName)
*   .setName(updatedName)
*   .setType(updatedType)
*   .setDescription(updatedDescription)
* .end()
* }}}
*
* '''Remove an ElementDefintion's PropertyDefinition'''
* {{{
* engine
*   .inSystemSpace()
*   .onElementDefinition(id)
*   .removePropertyDefinition(propName)
* .end()
* }}}
*
* '''Delete an ElementDefintion'''
* {{{
* engine
*   .inSystemSpace
*   .onElementDefinition(archPrinciple.id)
*   .delete()
* .end
* }}}
*
* ==User Space Usage==
* Manipulating element definitions in user space is identical to working in
* system space. The DSL simply requires that the scope is set to user space
* by specifying inUserSpace.
*
* '''Defining an ElementDefinition'''
* {{{
* engine
*   .inUserSpace
*   .defineElement(name, description)
*   .withProperty(pname, ptype, pdescription)
* .end
* }}}
*
* ==Dataset Usage==
* Manipulating element definitions in a dataset is identical to working in
* system space or user space. The DSL simply requires that the scope by
* specifying which dataset should be modified. This is accomplished with the
* functions [[org.machine.engine.graph.GraphDSL.onDataSet]] and
* [[org.machine.engine.graph.GraphDSL.onDataSetByName]]
*
* '''Create a DataSet'''
* {{{
* engine.createDataSet(datasetName, datasetDescription)
* }}}
*
* '''Find a DataSet by Name'''
* {{{
* val ds:DataSet = engine.findDataSetByName(datasetName)
* }}}
*
* '''Update a DataSet's Definition'''
* {{{
* engine
*   .onDataSet(id)
*   .setName(newName)
*   .setDescription(newDescription)
* .end
* }}}
*
* '''List available DataSets'''
* {{{
* val dataSetList:List[DataSet] = engine.datasets()
* }}}
*
* '''Create an ElementDefinition'''
* {{{
* engine
*   .onDataSetByName(datasetName)
*   .defineElement(elementName, elementDescription)
*     .withProperty(propName, propType, propDescription)
*     .withProperty(propName2, propType2, propDescription2)
* .end
* }}}
*
* ==Working with Datasets==
* Datasets are subgraphs of the overall system database. They represent groupings
* of graph nodes related to some domain. Datasets can be exported and imported
* for backups and collaboration.
*
* '''Find all elements in a dataset.'''
*
* ==Working with Elements==
* Elements exist inside of datasets. They can be created, updated and deleted.
*
* '''Creating an element from an element definition.'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .provision(elementDefinitionId)
*     .withField(fieldName, fieldDescription)
* .end
* }}}
*
* '''Update an Element'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .onElement(elementId)
*     .setField(fieldName1, fieldValue1)
*     .setField(fieldName2, fieldValue2)
*     .setField(fieldName3, fieldValue3)
* .end
* }}}
*
* '''Remove an Element's field'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .onElement(elementId)
*     .removeField(fieldName1, fieldValue1)
*     .removeField(fieldName2, fieldValue2)
*     .removeField(fieldName3, fieldValue3)
* .end
* }}}
*
* '''Delete an Element'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .onElement(elementId)
*   .delete
* .end
* }}}
*
* ==Working with Associations==
* Elements can be associated with each other. Associations can have properties
* assigned to them and managed just like elements.
*
* '''Creating an Association'''
* {{{
* val associationId = engine
*   .onDataSet(dataSetId)
*   .attach(startingElementId)
*   .to(endingElementId)
*   .as(associationType)
*   .withField(fieldName, fieldValue)
* .end
* }}}
*
* The as(associationType) is an optional clause. If it is not specified then the
* default association type of "is_associated_with" will be used.
* {{{
* val associationId = engine
*   .onDataSet(dataSetId)
*   .attach(startingElementId)
*   .to(endingElementId)
*   .withField(fieldName, fieldValue)
* .end
* }}}
*
* '''Find an Association'''
* {{{
* val association = engine
*   .onDataSet(dataSetId)
*   .findAssociation(associationId)
* }}}
*
* '''Update or Add Fields to an Association'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .onAssociation(associationId)
*   .setField(fieldName, fieldValue)
* .end
* }}}
*
* '''Remove Fields from an Association'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .onAssociation(associationId)
*   .removeField(fieldName, fieldValue)
*   .removeField(fieldName, fieldValue)
* .end
* }}}
*
* '''Remove an Association'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .onAssociation(associationId)
*   .delete
* .end
* }}}
*
* '''Find all outbound associations on an Element.'''
* Elements can be associated with other elements in the same dataset. Associations
* are directional edges in the graph. Since they are directional, the are considered
* inbound and outbound. Outbound associations can be found like below.
* {{{
* val associations = engine
*   .onDataSet(datasetId)
*   .onElement(elementId)
*   .findOutboundAssociations()
* }}}
*
* '''Find all inbound associations on an Element.'''
* {{{
* val associations = engine
*   .onDataSet(datasetId)
*   .onElement(elementId)
*   .findInboundAssociations()
* }}}
*
* '''Find Outbound Associated Elements'''
* {{{
* val children:List[Element] = engine
*   .onDataSet(datasetId)
*   .onElement(elementId)
*   .findDownStreamElements()
* }}}
*
* '''Find Inbound Associated Elements'''
* {{{
* val children:List[Element] = engine
*   .onDataSet(datasetId)
*   .onElement(elementId)
*   .findUpStreamElements()
* }}}
*
* ==Managing Identity==
* The system has the concept of a user which represents the person creating
* datasets. Users are automatically created in system space.
*
* '''Create a user.'''
* When creating a user the fields: firstName, lastName, emailAddress, and
* userName are require.
*
* {{{
* val userId = engine.createUser
*   .withFirstName(request.firstName)
*   .withLastName(request.lastName)
*   .withEmailAddress(request.emailAddress)
*   .withUserName(request.userName)
* .end
* }}}
*/
trait GraphDSL{
  //Abstract handlers
  def reset():GraphDSL
  def setUser(userId: String):GraphDSL
  def forUser(userId: String):GraphDSL
  def setScope(scope: CommandScope):GraphDSL
  def setActionType(actionType: ActionType):GraphDSL
  def setEntityType(entityType: EntityType):GraphDSL
  def setFilter(filter: Filter):GraphDSL
  def setOptions(options: GraphCommandOptions):GraphDSL
  def run:EngineCmdResult
  //End Abstract handlers

  def inSystemSpace():GraphDSL
  def inUserSpace():GraphDSL

  def createDataSet(name:String, description:String):String
  def datasets():Seq[DataSet]
  def onDataSet(id: String):GraphDSL
  def inDataSet(id: String):GraphDSL = onDataSet(id)
  def onDataSetByName(name: String):GraphDSL
  def findDataSetByName(name:String):DataSet
  def findDataSetById(id: String):DataSet

  def defineElement(name:String, description: String):GraphDSL
  def withProperty(name:String, ptype: String, description: String):GraphDSL
  def elementDefinitions():Seq[ElementDefinition]
  def findElementDefinitionById(id:String):ElementDefinition
  def findElementDefinitionByName(name:String):ElementDefinition

  def onElementDefinition(id: String):GraphDSL
  def setDescription(description: String):GraphDSL
  def setName(name:String):GraphDSL
  def setType(name:String):GraphDSL
  def editPropertyDefinition(name:String):GraphDSL
  def removePropertyDefinition(name: String):GraphDSL
  def delete():GraphDSL
  def end():String

  def provision(elementDefId: String):GraphDSL
  def elements():Seq[Element]
  def withField(fieldName: String, fieldValue: Any):GraphDSL
  def findElement(elementId: String):Element
  def onElement(elementId: String):GraphDSL
  def setField(fieldName: String, fieldValue: Any):GraphDSL

  def attach(startingElementId: String):GraphDSL
  def to(endingElementId: String):GraphDSL
  def as(associationName: String):GraphDSL
  def findAssociation(associationId: String):Association
  def onAssociation(annotationId: String):GraphDSL
  def removeField(fieldName: String):GraphDSL
  def findOutboundAssociations():Seq[Association]
  def findInboundAssociations():Seq[Association]
  def findDownStreamElements():Seq[Element]
  def findUpStreamElements():Seq[Element]

  def removeInboundAssociations()
  def removeOutboundAssociations()

  def createUser():GraphDSL
  def withFirstName(name:String):GraphDSL
  def withLastName(name:String):GraphDSL
  def withEmailAddress(email:String):GraphDSL
  def withUserName(email:String):GraphDSL
  def withUserPassword(password:String):GraphDSL
}

trait CmdResult{
}

case class ErrorResult(message:String) extends CmdResult
case class SuccesfullResult(message:String) extends CmdResult

object CmdResult{
  def error(msg: String):CmdResult = {
    new ErrorResult(msg)
  }

  def ok(msg: String):CmdResult = {
    new SuccesfullResult(msg)
  }
}
