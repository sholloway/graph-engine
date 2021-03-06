package org.machine.engine.graph.nodes

import org.machine.engine.exceptions._

/** An immutable element in a dataset.
* A node in the graph. It is created from the template of an
* ElementDefinition. The underlying Neo4J label is the
* ElementDefinition name. Fields are node properties based on
* the ElementDefinition's associated PropertyDefs.
*
* @constructor Creates a new Element.
* @param id The Element's unique identifier.
* @param elementType The type of element defintion the element is.
* @param elementDescription The definition that is derived from the related ElementDefinition.
* @param fields The data values associated with the element.
* @param creationTime When the element was created.
* @param lastModifiedTime When the element was last edited.
*/
case class Element(val id: String,
  val elementType: String,
  val elementDescription: String,
  val fields: Map[String, Any],
  val creationTime: String,
  val lastModifiedTime: String){

  override def toString():String = {
    val str = s"""
    |Element: $id $elementType
    |Fields: ${fields.size}
    |${fields.map{case (k,v) => s"\t$k: $v"}.mkString("\n")}
    """.stripMargin
    return str
  }

  /** Convience method for working with the element's fields.
  @param name The name of the field to retrieve.

  @example
  {{{
  val name = element.field[String]("name")
  }}}
  */
  def field[T](name:String):T = {
    val msg = "The element (%s) does not contain the field (%s)".format(id, name)
    if (!fields.contains(name)){
      throw new InternalErrorException(msg)
    }
    return fields.get(name).getOrElse(throw new InternalErrorException(msg)).asInstanceOf[T]
  }
}
