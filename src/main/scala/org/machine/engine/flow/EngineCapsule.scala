package org.machine.engine.flow

/**
An immutable internal data element passed between flows.
Intented to encapsulate generic data through the flow.
*/
trait EngineCapsule{
  /**
  The sequence of stops the capsule has been passed through.
  */
  def auditTrail: Seq[String]

  /**
  Returns a deep copy of the capsule with an additional
  audit entry.
  */
  def record(stop: String):EngineCapsule

  /**
  Returns a deep copy of the capsule with a new attributed added.
  */
  def enrich(key: String, value:Any, audit: Option[String] = None):EngineCapsule

  /**
  The associated attributes on the capsule.
  */
  def attributes:Map[String, Any]

  /**
  The current status of the capsule.
  */
  def status: EngineCapsuleStatus

  /**
  Return a deep copy of the capsule with the status set to the new value.
  */
  def setStatus(newStatus: EngineCapsuleStatus):EngineCapsule

  /**
  Optional associated error message. Only relevent if
  the status is EngineCapsuleStatuses.Error.
  */
  def errorMessage: Option[String]

  /**
  The associated message.
  */
  def message: ClientMessage

  /**
  The unique identifier of the capsule. Formated as a
  type IV UUID.
  */
  def id: String

  override def toString:String = {
    var msg: String = null.asInstanceOf[String]
    if (status == EngineCapsuleStatuses.Error){
      msg = s"""
      |EngineCapsule
      |ID: $id Status: $status
      |Error Message
      |${errorMessage.getOrElse("")}
      """.stripMargin
    }else{
      var itr = 0
      msg = s"""
      |EngineCapsule
      |ID: $id Status: $status
      |Audit Trail:
      |${auditTrail.map(i => {itr+=1; s"$itr: "+i;}).mkString(" ")}
      |Attributes:
      |${attributes.mkString("\n")}
      """.stripMargin
    }
    return msg
  }
}

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

object EngineCapsule{
  def apply(msg: ClientMessage, identifier: String):EngineCapsule = {
    new EngineCapsuleBase(
      auditTrail = Seq.empty[String],
      attributes = Map.empty[String, Any],
      status = EngineCapsuleStatuses.Ok,
      errorMessage = None,
      message = msg,
      id = identifier
    )
  }
}
