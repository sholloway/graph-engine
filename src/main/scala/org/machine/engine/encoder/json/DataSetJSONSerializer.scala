package org.machine.engine.encoder.json

import org.machine.engine.graph.nodes.DataSet

object DataSetJSONSerializer extends JSONSerializer[DataSet]{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  def serialize(results: Seq[DataSet]): String = {
    val json =
      ("datasets" ->
        results.map{ ds =>
          (
            ("id" -> ds.id) ~
            ("name" -> ds.name) ~
            ("description" -> ds.description) ~
            ("creationTime" -> ds.creationTime) ~
            ("lastModifiedTime" -> ds.lastModifiedTime)
          )
        }
      )
    return prettyRender(json)
  }
}
