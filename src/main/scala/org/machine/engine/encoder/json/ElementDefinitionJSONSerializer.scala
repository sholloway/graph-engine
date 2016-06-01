package org.machine.engine.encoder.json

import org.machine.engine.graph.nodes.ElementDefinition

object ElementDefinitionJSONSerializer extends JSONSerializer[ElementDefinition]{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  def serialize(results: Seq[ElementDefinition]): String = {
    val json =
      ("ElementDefinitions" ->
        results.map{ ed =>
          (
            ("id" -> ed.id) ~
            ("name" -> ed.name) ~
            ("description" -> ed.description) ~
            ("properties" ->
              ed.properties.map{ p =>
                (
                  ("id" -> p.id)~
                  ("name" -> p.name)~
                  ("type" -> p.propertyType)~
                  ("description" -> p.description)
                )
              }
            )
          )
        }
      )
    return prettyRender(json)
  }
}
