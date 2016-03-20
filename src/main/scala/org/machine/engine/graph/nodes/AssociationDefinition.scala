package org.machine.engine.graph.nodes

import org.machine.engine.exceptions._

/** The definition of an association between two elements in a dataset.
*
* @constructor Creates a new association definition.
* @param _associationType The association's type.
* @param _keys The names of the data values associated with the association.
*/
class AssociationDefinition(_associationType: String,
  _keys: List[String]){
  def associationType = this._associationType
  def keys = this._keys

  override def toString():String = {
    "AssociationDefinition: %s".format(associationType)
  }
}
