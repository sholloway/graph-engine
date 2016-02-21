package org.machine.engine.graph.nodes

import scala.collection.mutable.ListBuffer

/** A base element used to define what can be created by the user.
*
* @constructor Create a new Element Definition.
* @param _id: The Element Defintion NodeJS ID.
* @param _name: The Element Definition's name.
*/
class ElementDefinition(_id: String,
  _name: String){
  private val propertiesBuffer = new ListBuffer[PropertyDefinition]()

  def id = this._id
  def name = this._name
  def properties:List[PropertyDefinition] = {
    return propertiesBuffer.toList
  }

  def addProperty(property:PropertyDefinition):ElementDefinition = {
    this.propertiesBuffer += property
    return this
  }

  override def toString():String = {
    return "ElementDefinition: %s %s".format(id, name)
  }
}
