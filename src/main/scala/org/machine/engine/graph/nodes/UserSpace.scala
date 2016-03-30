package org.machine.engine.graph.nodes

import scala.collection.mutable.ArrayBuffer

/** The collection of ElementDefinitions the user defines.
*
* @constructor Creates a new UserSpace.
* @param id The UserSpace unique identifier.
* @param name The UserSpace name.
*/
class UserSpace(val id: String, val name: String){
  override def toString():String = {
    return s"$name: $id"
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
