package org.machine.engine.encoder.json

import org.machine.engine.graph.nodes.Association

object AssociationJSONSerializer extends JSONSerializer[Association]{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  def serialize(results: Seq[Association]): String = {
    val json =
      ("Associations" ->
        results.map{ a =>
          (
            ("id" -> a.id) ~
            ("associationType" -> a.associationType) ~
            ("startingElementId" -> a.startingElementId) ~
            ("endingElementId" -> a.endingElementId) ~
            ("creationTime" -> a.creationTime) ~
            ("lastModifiedTime" -> a.lastModifiedTime) ~
            ("fields" ->
              a.fields.toSeq.map{ case (k,v) => (s"$k" -> v.toString) }
            )
          )
        }
      )
    return prettyRender(json)
  }
}
/*
TODO Write matching test!
*/
