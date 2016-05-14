package org.machine.engine.flow

/**
Base implimentation of the EngineCapsule trait.
*/
class EngineCapsuleBase(
  val auditTrail: Seq[String],
  val attributes: Map[String, Any],
  val status: EngineCapsuleStatus,
  val errorMessage: Option[String],
  val message: ClientMessage,
  val id: String
) extends EngineCapsule{
  def enrich(key: String, value:Any, audit: Option[String] = None):EngineCapsule = {
    val newAtts = attributes.+(key -> value)
    val mutableAudit = auditTrail.toBuffer
    audit.foreach(stop => mutableAudit += stop)
    return new EngineCapsuleBase(
      mutableAudit.toSeq,
      newAtts,
      status,
      errorMessage,
      message,
      id)
  }

  def record(stop: String):EngineCapsule = {
    val stops = auditTrail:+(stop)
    return new EngineCapsuleBase(
      stops,
      attributes,
      status,
      errorMessage,
      message,
      id
    )
  }

  def setStatus(newStatus: EngineCapsuleStatus):EngineCapsule = {
    return new EngineCapsuleBase(
      auditTrail,
      attributes,
      newStatus,
      errorMessage,
      message,
      id
    )
  }
}
