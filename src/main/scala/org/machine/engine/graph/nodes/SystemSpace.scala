package org.machine.engine.graph.nodes

import scala.collection.mutable.ArrayBuffer

/** The collection of ElementDefinitions that are available to the user to
*   instantiate, but can not be redefined.
*
* @constructor Creates a new SystemSpace.
* @param _id The SystemSpace UUID.
* @param _name The SystemSpace name. This always says "System Space"
*/
class SystemSpace(_id: String, _name: String){
  def id = this._id
  def name = this._name

  override def toString():String = {
    return "%s: %s".format(name, id)
  }
}

object SystemSpace{
  def queryMapper(results: ArrayBuffer[SystemSpace],
    record: java.util.Map[java.lang.String, Object]):Unit = {
    val id:String = record.get("id").toString()
    val name:String = record.get("name").toString()
    results += new SystemSpace(id, name)
  }
}
