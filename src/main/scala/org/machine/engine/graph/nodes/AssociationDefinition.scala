package org.machine.engine.graph.nodes

import org.machine.engine.exceptions._

/** The definition of an association between two elements in a dataset.
*
* @constructor Creates a new association definition.
* @param associationType The association's type.
* @param keys The names of the data values associated with the association.
*/
class AssociationDefinition(val associationType: String,
  val keys: List[String]){

  override def toString():String = {
    return s"AssociationDefinition: $associationType"
  }
}
