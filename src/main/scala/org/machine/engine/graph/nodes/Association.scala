package org.machine.engine.graph.nodes

import org.machine.engine.exceptions._

/** An immutable association between two elements in a dataset.
*
* @constructor Creates a new association.
* @param id The association's unique identifier.
* @param associationType The association's type.
* @param fields The data values associated with the association.
* @param creationTime When the association was created.
* @param lastModifiedTime When the association was last edited.
*/
class Association(val id: String,
  val associationType: String,
  val fields: Map[String, Any],
  val startingElementId: String,
  val endingElementId: String,
  val creationTime: String,
  val lastModifiedTime: String){

  override def toString():String = {
    return s"Association: $id $associationType"
  }

  /** Convience method for working with the element's fields.
  @param name The name of the field to retrieve.

  @example
  {{{
  val probability = association.field[Double]("probability")
  }}}
  */
  def field[T](name:String):T = {
    val msg = "The association (%s) does not contain the field (%s)".format(id, name)
    if (!fields.contains(name)){
      throw new InternalErrorException(msg)
    }
    return fields.get(name).getOrElse(throw new InternalErrorException(msg)).asInstanceOf[T]
  }
}
