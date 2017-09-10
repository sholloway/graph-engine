package org.machine.engine.flow.requests

case class RequestMessage(userId: String,
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

  def parseJSON(msg: String):RequestMessage = {
    val json = parse(msg)
    val userId = (json \ "userId").extract[String]
    val actionType = (json \ "actionType").extract[String]
    val scope = (json \ "scope").extract[String]
    val entityType = (json \ "entityType").extract[String]
    val filter = (json \ "filter").extract[String]
    val jsonMap = json.values.asInstanceOf[Map[String, Any]]
    val options = mutable.Map.empty[String, Any]

    if(jsonMap.contains("options")){
      val jsonOptions = jsonMap("options").asInstanceOf[Map[String, Any]]
      val excluded = Seq("properties")
      jsonOptions.foreach(field => {
        if (!excluded.contains(field._1)){
          options += (field._1 -> field._2)
        }
      })
      if (jsonOptions.contains("properties")){
        val jsonProps = jsonOptions("properties").asInstanceOf[Seq[Map[String, String]]]
        options += ("properties" -> jsonProps)
      }
    }
    RequestMessage(userId, actionType, scope, entityType, filter, options.toMap)
  }

  def jsonToMap(json: String):Map[String, Any] ={
    val jsonDom = parse(json)
    println(jsonDom)
    return jsonDom.values.asInstanceOf[Map[String, Any]]
  }

  /*
  Checks for the presence of all required fields.

  Returns:
  Tuple: (fully defined, optional error message)
  */
  def isFullyDefined(msg: RequestMessage):(Boolean,Option[String])  = {
    val response:(Boolean, Option[String]) = if (msg.userId == null){
      (false, Some("The request must contain a valid user ID."))
    }else if(msg.actionType == null){
      (false, Some("The request must contain a valid action type."))
    }else if(msg.scope == null){
      (false, Some("The request must contain a valid scope."))
    }else if(msg.entityType == null){
      (false, Some("The request must contain a valid entity type."))
    }else if(msg.filter == null){
      (false, Some("The request must contain a valid filter."))
    }else{
      (true, None)
    }
    return response
  }

  private def fetchString(str: String, options: mutable.Map[String, Any], jsonOptions: Map[String, Any]) = {
    if (jsonOptions.contains(str)){
      options += (str->jsonOptions(str))
    }
  }
}
