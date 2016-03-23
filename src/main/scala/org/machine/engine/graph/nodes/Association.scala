package org.machine.engine.graph.nodes

import org.machine.engine.exceptions._

/** An immutable association between two elements in a dataset.
*
* @constructor Creates a new association.
* @param _id The association's unique identifier.
* @param _associationType The association's type.
* @param _fields The data values associated with the association.
* @param _creationTime When the association was created.
* @param _lastModifiedTime When the association was last edited.
*/
class Association(_id: String,
  _associationType: String,
  _fields: Map[String, Any],
  _startingElementId: String,
  _endingElementId: String,
  _creationTime: String,
  _lastModifiedTime: String){
  def id = this._id
  def associationType = this._associationType
  def fields = this._fields
  def startingElementId = this._startingElementId
  def endingElementId = this._endingElementId
  def creationTime = this._creationTime
  def lastModifiedTime = this._lastModifiedTime

  override def toString():String = {
    "Element: %s %s".format(id, associationType)
  }

  /** Convience method for working with the element's fields.
  @param name The name of the field to retrieve.

  @example
  {{{
  val probability = association.field[Double]("probability")
  }}}
  */
  def field[T](name:String):T = {
    val msg = "The association (%s) does not contain the field (%s)".format(id, name)
    if (!fields.contains(name)){
      throw new InternalErrorException(msg)
    }
    return fields.get(name).getOrElse(throw new InternalErrorException(msg)).asInstanceOf[T]
  }
}
