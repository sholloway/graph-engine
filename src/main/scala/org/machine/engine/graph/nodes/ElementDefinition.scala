package org.machine.engine.graph.nodes

/** A base element used to define what can be created by the user.
*
* @constructor Create a new Element Definition.
* @param _id: The Element Defintion NodeJS ID.
* @param _name: The Element Definition's name.
*/
class ElementDefinition(_id: Long, _name: String){
  def id = this._id
  def name = this._name

  override def toString():String = {
    return id.toString() + " " + name
  }
}
