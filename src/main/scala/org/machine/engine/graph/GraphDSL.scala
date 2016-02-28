package org.machine.engine.graph

import org.machine.engine.graph.nodes._

/** Definition of internal DSL for working with the underling graph database.
*/
trait GraphDSL{
  def inSystemSpace():GraphDSL
  def inUserSpace():GraphDSL
  def defineElement(name:String, description: String):GraphDSL
  def withProperty(name:String, ptype: String, description: String):GraphDSL
  def elements():List[ElementDefinition]
  def findElementDefinitionById(id:String):ElementDefinition
  def findElementDefinitionByName(name:String):ElementDefinition
  def onElementDefinition(id: String):GraphDSL
  def setDescription(description: String):GraphDSL
  def setName(name:String):GraphDSL
  def setType(name:String):GraphDSL
  def editPropertyDefinition(name:String):GraphDSL
  def removePropertyDefinition(name: String):GraphDSL
  def delete():GraphDSL
  def end():GraphDSL
}
