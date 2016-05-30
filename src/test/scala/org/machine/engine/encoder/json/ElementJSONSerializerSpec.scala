package org.machine.engine.encoder.json

import org.scalatest._
import org.scalatest.mock._

import org.machine.engine.graph.nodes.{Element}

class ElementJSONSerializerSpec extends FunSpec with Matchers with EasyMockSugar{
  describe("Serializing Sequence of ElementDefinitions"){
    it ("should serialze a sequence of element definitions"){
      val elements = Seq(Element("1", "e1", "e1 desc", Map.empty[String,Any], "yesterday", "today"),
        Element("2", "e2", "e2 desc", Map.empty[String,Any], "yesterday", "today"),
        Element("3", "e3", "e3 desc", Map.empty[String,Any], "yesterday", "today"))

      val json = ElementJSONSerializer.serialize(elements)
      val expected = """
      |{
      |  "Elements":[
      |    {
      |      "id":"1",
      |      "elementType":"e1",
      |      "description":"e1 desc",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[]
      |    },
      |    {
      |      "id":"2",
      |      "elementType":"e2",
      |      "description":"e2 desc",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[]
      |    },
      |    {
      |      "id":"3",
      |      "elementType":"e3",
      |      "description":"e3 desc",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[]
      |    }
      |  ]
      |}
      """
      strip(json) should equal(strip(expected))
    }

    it ("should serialze a sequence of element definitions with property definitions"){
      val fields = Map("a"->123, "b"->"http://fakeurl.com", "c"->"adsf", "d"->42f)
      val elements = Seq(Element("1", "e1", "e1 desc", fields, "yesterday", "today"),
        Element("2", "e2", "e2 desc", Map.empty[String,Any], "yesterday", "today"),
        Element("3", "e3", "e3 desc", Map.empty[String,Any], "yesterday", "today"))

      val json = ElementJSONSerializer.serialize(elements)
      val expected = """
      |{
      |  "Elements":[
      |    {
      |      "id":"1",
      |      "elementType":"e1",
      |      "description":"e1 desc",
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
      |      "elementType":"e2",
      |      "description":"e2 desc",
      |      "creationTime":"yesterday",
      |      "lastModifiedTime":"today",
      |      "fields":[]
      |    },
      |    {
      |      "id":"3",
      |      "elementType":"e3",
      |      "description":"e3 desc",
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
