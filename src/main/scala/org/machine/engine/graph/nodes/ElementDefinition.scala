package org.machine.engine.graph.nodes

import scala.collection.mutable.ListBuffer

/** A base element used to define what can be created by the user.
*
* @constructor Create a new Element Definition.
* @param id The Element Defintion NodeJS ID.
* @param name The Element Definition's name.
*/
case class ElementDefinition(val id: String,
  val name: String,
  val description: String){
  private val propertyDefs = new PropertyDefinitions()
  def properties:List[PropertyDefinition] = propertyDefs.toList

  def addProperty(property:PropertyDefinition):ElementDefinition = {
    this.propertyDefs.addProperty(property)
    return this
  }

  override def toString():String = {
    return s"ElementDefinition: $id $name"
  }
}
