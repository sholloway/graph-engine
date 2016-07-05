package org.machine.engine.encoder.json

import org.scalatest._
import org.scalatest.mock._
import org.machine.engine.graph.commands.QueryCmdResult
import org.machine.engine.graph.nodes.{Association}

class AssociationJSONSerializerSpec extends FunSpec with Matchers with EasyMockSugar{
  describe("Serializing Sequence of Associations"){
    it ("should serialze a sequence of element definitions"){
      val elements = Seq(Association("1", "a1", Map.empty[String,Any], "start","stop","yesterday", "today"),
        Association("2", "a2", Map.empty[String,Any], "start","stop", "yesterday", "today"),
        Association("3", "a3", Map.empty[String,Any], "start","stop", "yesterday", "today"))

      val result = QueryCmdResult[Association](elements)
      val json = AssociationJSONSerializer.serialize(result, elements)
      val expected = """
      |{
      |  "status": "OK",
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

    it("should serialze a sequence of element definitions with property definitions"){
      val fields = Map("a"->123, "b"->"http://fakeurl.com", "c"->"adsf", "d"->42f)
      val elements = Seq(Association("1", "a1", fields, "start","stop","yesterday", "today"),
        Association("2", "a2", Map.empty[String,Any], "start","stop", "yesterday", "today"),
        Association("3", "a3", Map.empty[String,Any], "start","stop", "yesterday", "today"))

      val result = QueryCmdResult[Association](elements)
      val json = AssociationJSONSerializer.serialize(result, elements)
      val expected = """
      |{
      |  "status": "OK",
      |  "Associations":[
      |    {
      |      "id":"1",
      |      "associationType":"a1",
      |      "startingElementId":"start",
      |      "endingElementId":"stop",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[
      |        {
      |          "a":"123"
      |        },
      |        {
      |          "b":"http://fakeurl.com"
      |        },
      |        {
      |          "c":"adsf"
      |        },
      |        {
      |          "d":"42.0"
      |        }
      |      ]
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
  }

  def strip(str: String):String = str.stripMargin.replaceAll("\t","").replaceAll(" ","").replaceAll("\n","")
}
