package org.machine.engine.graph.nodes

/** A property associated with an ElementDefinition.
*
* @constructor Create a new PropertyDefinition.
* @param _id The PropertyDefinition NodeJS ID.
* @param _name The PropertyDefinition's name.
* @param _type The PropertyDefinition's type.
* @param _description The PropertyDefinition's description.
*/
class PropertyDefinition(_id: String, _name: String, _type: String, _description: String){
  def id = this._id
  def name = this._name
  def propertyType = this._type
  def description = this._description

  override def toString():String = {
    return "PropertyDefinition: %s | %s | %s | %s".format(id, name, propertyType, description)
  }
}
