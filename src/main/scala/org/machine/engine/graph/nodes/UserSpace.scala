package org.machine.engine.graph.nodes

import scala.collection.mutable.ArrayBuffer

/** The collection of ElementDefinitions the user defines.
*
* @constructor Creates a new UserSpace.
* @param _id The UserSpace unique identifier.
* @param _name The UserSpace name.
*/
class UserSpace(_id: String, _name: String){
  def id = this._id
  def name = this._name

  override def toString():String = {
    return "%s: %s".format(name, id)
  }
}

object UserSpace{
  def queryMapper(results: ArrayBuffer[UserSpace],
    record: java.util.Map[java.lang.String, Object]):Unit = {
    val id:String = record.get("id").toString()
    val name:String = record.get("name").toString()
    results += new UserSpace(id, name)
  }
}
