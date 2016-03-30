package org.machine.engine.graph.nodes

import scala.collection.mutable.ArrayBuffer

/** The collection of ElementDefinitions that are available to the user to
*   instantiate, but can not be redefined.
*
* @constructor Creates a new SystemSpace.
* @param id The SystemSpace UUID.
* @param name The SystemSpace name. This always says "System Space"
*/
class SystemSpace(val id: String, val name: String){
  override def toString():String = {
    return s"$name: $id"
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
