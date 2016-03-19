package org.machine.engine.graph.nodes

import scala.collection.mutable.ListBuffer
class PropertyDefinitions{
  val definitions = ListBuffer.empty[PropertyDefinition]

  def addProperty(prop: PropertyDefinition):PropertyDefinitions = {
    definitions += prop
    return this
  }

  def toList:List[PropertyDefinition] = definitions.toList
}
