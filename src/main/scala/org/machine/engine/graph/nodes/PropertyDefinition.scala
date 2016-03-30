package org.machine.engine.graph.nodes

/** A property associated with an ElementDefinition.
*
* @constructor Create a new PropertyDefinition.
* @param id The PropertyDefinition NodeJS ID.
* @param name The PropertyDefinition's name.
* @param type The PropertyDefinition's type.
* @param description The PropertyDefinition's description.
*/
class PropertyDefinition(val id: String,
  val name: String,
  val propertyType: String,
  val description: String){

  override def toString():String = {
    return s"PropertyDefinition: $id | $name | $propertyType | $description"
  }

  def toMap:Map[String, String] = Map("mid" -> id,
    "name" -> name,
    "type" -> propertyType,
    "description" -> description)
}
