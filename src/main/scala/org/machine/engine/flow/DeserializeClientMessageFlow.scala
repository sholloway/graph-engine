package org.machine.engine.flow

import scala.util.{Either, Left, Right}
import org.machine.engine.flow.requests._
import net.liftweb.json._
import net.liftweb.json.Extraction._

object DeserializeClientMessage{
  def deserialize(capsule: EngineCapsule):EngineCapsule = {
    val serializedMsg = capsule.message.payload
    implicit val formats = net.liftweb.json.DefaultFormats
    val sanatized = net.liftweb.json.prettyRender(decompose(serializedMsg))
    val requestMsg = RequestMessage.parseJSON(serializedMsg)
    val requestEvaluation:(Boolean, Option[String]) = RequestMessage.isFullyDefined(requestMsg)
    val transformedCapsule: EngineCapsule = if (requestEvaluation._1){
      capsule.enrich("deserializedMsg", requestMsg, Some("deserializeRequest"))
    }else{
      capsule.setStatus(EngineCapsuleStatuses.Error, Some(requestEvaluation._2.get))
    }
    return transformedCapsule
  }
}
