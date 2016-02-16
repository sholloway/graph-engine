package org.machine.engine.graph.nodes

/** The collection of ElementDefinitions the user defines.
*
* @constructor Creates a new UserSpace.
* @param _id: The UserSpace NodeJS ID.
*/
class UserSpace(_id: Long){
  def id = this._id

  override def toString():String = {
    return "User Space:" + id.toString()
  }
}
