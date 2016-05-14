package org.machine.engine.flow

import scala.util.Random

object ValidateClientMessage{
  /*
  For now just randomly fail a message.
  Expects implicit rng = new Random(...)
  */
  def validate(capsule: EngineCapsule)(implicit rng:Random):EngineCapsule = {
    val newStatus = if (rng.nextBoolean) EngineCapsuleStatuses.Ok else EngineCapsuleStatuses.Error
    return capsule.setStatus(newStatus)
  }
}
