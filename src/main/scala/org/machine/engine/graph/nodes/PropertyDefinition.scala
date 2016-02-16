package org.machine.engine.graph.nodes

/** A property associated with an ElementDefinition.
*
* @constructor Create a new PropertyDefinition.
* @param _id: The PropertyDefinition NodeJS ID.
* @param _name: The PropertyDefinition's name.
* @param _type: The PropertyDefinition's type.
*/
class PropertyDefinition(_id: Long, _name: String, _type: String){
  def id = this._id
  def name = this._name
  def propertyType = this._type

  override def toString():String = {
    return id.toString() + " " + name
  }
}
