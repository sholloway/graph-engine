package org.machine.engine.encoder.json

import org.machine.engine.graph.nodes.Element

object ElementJSONSerializer{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  def serialize(results: Seq[Element]): String = {
    val json =
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
        }
      )
    return prettyRender(json)
  }
}
