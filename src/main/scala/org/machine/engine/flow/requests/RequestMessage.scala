package org.machine.engine.flow.requests

case class RequestMessage(user: String,
  actionType: String,
  scope: String,
  entityType: String,
  filter: String,
  options: Map[String, Any] = Map.empty[String, Any]
)

import net.liftweb.json._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.{read, write}
import scala.collection.mutable

object RequestMessage{
  // implicit val formats = Serialization.formats(DefaultFormats)
  implicit val formats = DefaultFormats
  def fromJSON(msg: String):RequestMessage = {
    return read[RequestMessage](msg)
  }

  def toJSON(rm: RequestMessage):String = {
    return write(rm)
  }

  def parseJSON(msg: String):RequestMessage ={
    val json = parse(msg)

    val user = (json \ "user").asInstanceOf[JString].values
    val actionType = (json \ "actionType").asInstanceOf[JString].values
    val scope = (json \ "scope").asInstanceOf[JString].values
    val entityType = (json \ "entityType").asInstanceOf[JString].values
    val filter = (json \ "filter").asInstanceOf[JString].values

    val jsonMap = json.values.asInstanceOf[Map[String, Any]]
    val options = mutable.Map.empty[String, Any]

    if(jsonMap.contains("options")){
      val jsonOptions = jsonMap("options").asInstanceOf[Map[String, Any]]
      fetchString("mid", options, jsonOptions)
      fetchString("dsId", options, jsonOptions)
      fetchString("dsName", options, jsonOptions)
      fetchString("pname", options, jsonOptions)
      fetchString("name", options, jsonOptions)
      fetchString("description", options, jsonOptions)
      if (jsonOptions.contains("properties")){
        val jsonProps = jsonOptions("properties").asInstanceOf[Seq[Map[String, String]]]
        options += ("properties" -> jsonProps)
      }
    }

    RequestMessage(user, actionType, scope, entityType, filter, options.toMap)
  }

  def jsonToMap(json: String):Map[String, Any] ={
    val jsonDom = parse(json)
    return jsonDom.values.asInstanceOf[Map[String, Any]]
  }

  private def fetchString(str: String, options: mutable.Map[String, Any], jsonOptions: Map[String, Any]) = {
    if (jsonOptions.contains(str)){
      options += (str->jsonOptions(str))
    }
  }
}
