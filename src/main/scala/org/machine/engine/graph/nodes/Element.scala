package org.machine.engine.graph.nodes

/** An immutable element in a dataset.
*
* @constructor Creates a new Element.
* @param _id The Element's unique identifier.
* @param _elementType The type of element defintion the element is.
* @param _elementDescription The definition that is derived from the related ElementDefinition.
* @param _fields The data values associated with the element.
* @param _createTime When the element was created.
* @param _lastModifiedTime When the element was last edited.
*/
class Element(_id: String,
  _elementType: String,
  _elementDescription: String,
  _fields: Map[String, AnyRef],
  _creationTime: String,
  _lastModifiedTime: String){
  def id = this._id
  def elementType = this._elementType
  def elementDescription = this._elementDescription
  def fields = this._fields
  def creationTime = this._creationTime
  def lastModifiedTime = this._lastModifiedTime

  override def toString():String = {
    "Element: %s %s".format(id, elementType)
  }
}
