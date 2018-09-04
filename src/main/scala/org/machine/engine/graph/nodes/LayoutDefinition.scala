package org.machine.engine.graph.nodes

/** An abstract object that is responsible for defining visual layouts.
*
* @constructor Creates a new DataSet.
* @param id The LayoutDefinition node ID.
* @param name The LayoutDefinition's name.
* @param description The LayoutDefinition's description.
* @param createTime When the LayoutDefinition was created.
* @param lastModifiedTime When the LayoutDefinition was last edited.
*/
case class LayoutDefinition(val id: String,
  val name: String,
  val description: String,
  val creationTime: String,
  val lastModifiedTime: String){

  override def toString():String = {
    "DataSet: %s %s".format(id.toString(), name)
  }
}
