package org.machine.engine.encoder.json

import org.machine.engine.graph.commands.EngineCmdResult
import org.machine.engine.graph.nodes.Association

object AssociationJSONSerializer extends JSONSerializer[Association]{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  def serialize(result: EngineCmdResult, results: Seq[Association]): String = {
    val json =
      (
        ("status" -> result.status.value) ~
        ("errorMessage" -> result.errorMessage) ~
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
        })
      )
    return prettyRender(json)
  }
}
/*
TODO Write matching test!
*/
