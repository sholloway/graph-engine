package org.machine.engine.flow

import scala.util.{Either, Left, Right}
import org.machine.engine.flow.requests._

object DeserializeClientMessage{
  /*
  Stop immediately if the message is invalid
  Validate Core
    Validate User
    Validate ActionType
    Validate Scope
    Validate EntityType
    Validate Filter

  Validate Command Specific Options 
    At this point, do I traverse the tree and find the Decision?
  */
  def deserialize(capsule: EngineCapsule):EngineCapsule = {
    val serializedMsg = capsule.message.payload
    val jsonMap = RequestMessage.jsonToMap(serializedMsg)
    val transformedCapsule: EngineCapsule = RequestRuleValidator.validate(Left(jsonMap)) match{
      case Left(jmap) =>{
        val requestMsg = RequestMessage.parseJSON(serializedMsg)
        capsule.enrich("deserializedMsg", requestMsg, Some("deserializeRequest"))
      }
      case Right(errorMsg) => {
        capsule.setStatus(EngineCapsuleStatuses.Error, Some(errorMsg))
      }
    }
    return transformedCapsule
  }

  val validateUser = new PartialFunction[Map[String, Any], Boolean]{
    def apply(jsonMap: Map[String, Any]):Boolean = {
      return jsonMap.contains("user")
    }
    def isDefinedAt(jsonMap: Map[String, Any]) = true
  }

  val validateActionType = new PartialFunction[Map[String, Any], Boolean]{
    def apply(jsonMap: Map[String, Any]):Boolean = {
      return jsonMap.contains("actionType")
    }
    def isDefinedAt(jsonMap: Map[String, Any]) = true
  }
}
