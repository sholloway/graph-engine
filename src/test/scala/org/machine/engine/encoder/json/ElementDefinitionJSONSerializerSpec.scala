package org.machine.engine.encoder.json

import org.scalatest._
import org.scalatest.mock._

import org.machine.engine.graph.nodes.{ElementDefinition, PropertyDefinition}

class ElementDefinitionJSONSerializerSpec extends FunSpec with Matchers with EasyMockSugar{
  describe("Serializing Sequence of ElementDefinitions"){
    it ("should serialze a sequence of element definitions"){
      val defs = Seq(ElementDefinition("1", "ed1", "ed1 desc"),
        ElementDefinition("2", "ed2", "ed2 desc"),
        ElementDefinition("3", "ed3", "ed3 desc"))

      val json = ElementDefinitionJSONSerializer.serialize(defs)
      val expected = """
      |{
      |  "ElementDefinitions":[
      |    {
      |      "id":"1",
      |      "name":"ed1",
      |      "description":"ed1 desc",
      |      "properties":[]
      |    },
      |    {
      |      "id":"2",
      |      "name":"ed2",
      |      "description":"ed2 desc",
      |      "properties":[]
      |    },
      |    {
      |      "id":"3",
      |      "name":"ed3",
      |      "description":"ed3 desc",
      |      "properties":[]
      |    }
      |  ]
      |}
      """
      strip(json) should equal(strip(expected))
    }

    it ("should serialze a sequence of element definitions with property definitions"){
      val defs = Seq(ElementDefinition("1", "ed1", "ed1 desc")
          .addProperty(PropertyDefinition("11", "pd11", "String", "pd11 desc"))
          .addProperty(PropertyDefinition("12", "pd12", "String", "pd12 desc")),
        ElementDefinition("2", "ed2", "ed2 desc"),
        ElementDefinition("3", "ed3", "ed3 desc"))

      val json = ElementDefinitionJSONSerializer.serialize(defs)

      val expected = """
      |{
      |  "ElementDefinitions":[
      |    {
      |      "id":"1",
      |      "name":"ed1",
      |      "description":"ed1 desc",
      |      "properties":[
      |        {
      |          "id":"11",
      |          "name":"pd11",
      |          "type":"String",
      |          "description":"pd11 desc"
      |        },
      |        {
      |          "id":"12",
      |          "name":"pd12",
      |          "type":"String",
      |          "description":"pd12 desc"
      |        }
      |      ]
      |    },
      |    {
      |      "id":"2",
      |      "name":"ed2",
      |      "description":"ed2 desc",
      |      "properties":[]
      |    },
      |    {
      |      "id":"3",
      |      "name":"ed3",
      |      "description":"ed3 desc",
      |      "properties":[]
      |    }
      |  ]
      |}
      """
      strip(json) should equal(strip(expected))
    }
  }

  def strip(str: String):String = str.stripMargin.replaceAll("\t","").replaceAll(" ","").replaceAll("\n","")
}
