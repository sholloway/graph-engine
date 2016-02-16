package org.machine.engine.graph.nodes

/** The collection of ElementDefinitions that are available to the user to
*   instantiate, but can not be redefined.
*
* @constructor Creates a new SystemSpace.
* @param _id: The SystemSpace NodeJS ID.
*/
class SystemSpace(_id: Long){
  def id = this._id

  override def toString():String = {
    return "System Space:" + id.toString()
  }
}
