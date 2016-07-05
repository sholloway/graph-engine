package org.machine.engine.encoder.json

import org.machine.engine.graph.commands.EngineCmdResult
import org.machine.engine.graph.nodes.Element

object ElementJSONSerializer extends JSONSerializer[Element]{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  def serialize(result: EngineCmdResult, results: Seq[Element]): String = {
    val json =
      (
        ("status" -> result.status.value) ~
        ("errorMessage" -> result.errorMessage) ~
        ("Elements" ->
        results.map{ e =>
          (
            ("id" -> e.id) ~
            ("elementType" -> e.elementType) ~
            ("description" -> e.elementDescription) ~
            ("creationTime" -> e.creationTime) ~
            ("lastModifiedTime" -> e.lastModifiedTime) ~
            ("fields" ->
              e.fields.toSeq.map{ case (k,v) => (s"$k" -> v.toString) }
            )
          )
        })
      )
    return prettyRender(json)
  }
}
