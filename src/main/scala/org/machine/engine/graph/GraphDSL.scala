package org.machine.engine.graph

import org.machine.engine.graph.nodes._

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
* The graph is composed of ElementDefinitions and Elements. An ElementDefinition
* is a definition of an element that can be provisioned. An Element is an instance
* of an ElementDefinition.
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
* '''Delete an Element'''
* {{{
* engine
*   .onDataSet(dataSetId)
*   .onElement(elementId)
*   .delete
* .end
* }}}
*/
trait GraphDSL{
  def inSystemSpace():GraphDSL
  def inUserSpace():GraphDSL

  def createDataSet(name:String, description:String):String
  def datasets():List[DataSet]
  def onDataSet(id: String):GraphDSL
  def onDataSetByName(name: String):GraphDSL
  def findDataSetByName(name:String):DataSet
  def findDataSetById(id: String):DataSet

  def defineElement(name:String, description: String):GraphDSL
  def withProperty(name:String, ptype: String, description: String):GraphDSL
  def elementDefinitions():List[ElementDefinition]
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
}
