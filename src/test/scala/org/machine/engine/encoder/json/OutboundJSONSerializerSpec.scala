package org.machine.engine.encoder.json

import org.scalatest._
import org.scalatest.mock._

import org.machine.engine.graph.nodes.{Association}
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult, QueryCmdResult, UpdateCmdResult, DeleteCmdResult, InsertCmdResult}

class OutboundJSONSerializerSpec extends FunSpec with Matchers with EasyMockSugar{
  describe("Serializing Outbound JSON Messages"){
    it ("should serialze a QueryCmdResult"){
      val elements = Seq(Association("1", "a1", Map.empty[String,Any], "start","stop","yesterday", "today"),
        Association("2", "a2", Map.empty[String,Any], "start","stop", "yesterday", "today"),
        Association("3", "a3", Map.empty[String,Any], "start","stop", "yesterday", "today"))

      val json = OutboundJSONSerializer.serialize(QueryCmdResult[Association](elements))
      val expected = """
      |{
      |  "Associations":[
      |    {
      |      "id":"1",
      |      "associationType":"a1",
      |      "startingElementId":"start",
      |      "endingElementId":"stop",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[]
      |    },
      |    {
      |      "id":"2",
      |      "associationType":"a2",
      |      "startingElementId":"start",
      |      "endingElementId":"stop",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[]
      |    },
      |    {
      |      "id":"3",
      |      "associationType":"a3",
      |      "startingElementId":"start",
      |      "endingElementId":"stop",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[]
      |    }
      |  ]
      |}
      """
      strip(json) should equal(strip(expected))
    }

    it ("should serialize a UpdateCmdResult"){
      val json = OutboundJSONSerializer.serialize(UpdateCmdResult[String]("identifier"))
      val expected = """
      |{
      | "id": "identifier"
      |}
      """
      strip(json) should equal(strip(expected))
    }

    it ("should serialize a DeleteCmdResult"){
      val json = OutboundJSONSerializer.serialize(DeleteCmdResult[String]("identifier"))
      val expected = """
      |{
      | "id": "identifier"
      |}
      """
      strip(json) should equal(strip(expected))
    }

    it ("should serialize a InsertCmdResult"){
      val json = OutboundJSONSerializer.serialize(InsertCmdResult[String]("identifier"))
      val expected = """
      |{
      | "id": "identifier"
      |}
      """
      strip(json) should equal(strip(expected))
    }
  }

  def strip(str: String):String = str.stripMargin.replaceAll("\t","").replaceAll(" ","").replaceAll("\n","")
}
