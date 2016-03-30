package org.machine.engine.graph.nodes

/** A graph, defined by a user.
*
* @constructor Creates a new DataSet.
* @param id The DataSet NodeJS ID.
* @param name The DataSet name.
* @param description The DataSet description.
* @param createTime When the DataSet was created.
* @param lastModifiedTime When the DataSet was last edited.
*/
class DataSet(val id: String,
  val name: String,
  val description: String,
  val creationTime: String,
  val lastModifiedTime: String){ 

  override def toString():String = {
    "DataSet: %s %s".format(id.toString(), name)
  }
}
