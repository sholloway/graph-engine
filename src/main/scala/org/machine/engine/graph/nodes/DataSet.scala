package org.machine.engine.graph.nodes

/** A graph, defined by a user.
*
* @constructor Creates a new DataSet.
* @param _id: The DataSet NodeJS ID.
* @param _name: The DataSet name.
* @param _description: The DataSet description.
* @param _createTime: When the DataSet was created.
* @param _lastModifiedType: When the DataSet was last edited.
*/
class DataSet(_id: String,
  _name: String,
  _description: String,
  _creationTime: String, //change to some kind of time type.
  _lastModifiedType: String){
  def id = this._id
  def name = this._name
  def description = this._description
  def creationTime = this._creationTime
  def lastModifiedType = this._lastModifiedType

  override def toString():String = {
    "DataSet: %s %s".format(id.toString(), name)
  }
}
