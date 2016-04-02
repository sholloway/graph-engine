package org.machine.engine.logger

sealed trait LoggerLevel{
  def value: Int
  def display: String
  def <=(level:LoggerLevel):Boolean = {
    return value <= level.value
  }
}

object LoggerLevels{
  case object DEBUG extends LoggerLevel{ val value = 0; val display = "DEBUG";}
  case object INFO extends LoggerLevel{ val value = 1; val display = "INFO";}
  case object WARNING extends LoggerLevel{ val value = 2; val display = "WARNING";}
  case object ERROR extends LoggerLevel{ val value = 3; val display = "ERROR";}
  case object CRITICAL extends LoggerLevel{ val value = 4; val display = "CRITICAL";}
}
