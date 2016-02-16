package org.machine.engine.logger

sealed trait LoggerLevel{
  def value: Int
  def <=(level:LoggerLevel):Boolean = {
    return value <= level.value
  }
}

object LoggerLevels{
  case object DEBUG extends LoggerLevel{ val value = 0;}
  case object INFO extends LoggerLevel{ val value = 1;}
  case object WARNING extends LoggerLevel{ val value = 2;}
  case object ERROR extends LoggerLevel{ val value = 3;}
  case object CRITICAL extends LoggerLevel{ val value = 4;}
}
