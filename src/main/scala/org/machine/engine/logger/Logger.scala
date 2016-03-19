package org.machine.engine.logger

import java.util.Calendar
import java.text.SimpleDateFormat
class Logger(_level: LoggerLevel){
  var internalLevel:LoggerLevel = null
  val timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS z")
  setLevel(_level)

  def level = this.internalLevel

  def setLevel(newLevel: LoggerLevel) = {
    this.internalLevel = newLevel
  }

  def info(msg: String) = {
    if (level <= LoggerLevels.INFO){
      log("INFO", now, msg)
    }
  }

  def debug(msg: String) = {
    if (level <= LoggerLevels.DEBUG){
      log("DEBUG", now, msg)
    }
  }

  def warn(msg: String) = {
    if (level <= LoggerLevels.WARNING){
      log("WARNING", now, msg)
    }
  }

  def error(msg: String) = {
    if (level <= LoggerLevels.ERROR){
      log("ERROR", now, msg)
    }
  }

  def critical(msg: String) = {
    if (level <= LoggerLevels.CRITICAL){
      log("CRITICAL", now, msg)
    }
  }

  private def log(logType:String, time:String, msg: String) = {
    val str = "%s: %s - %s".format(logType, time, msg)
    this.println(str)
  }

  def println(msg:String) = {
    Console.println(msg)
  }

  private def now():String = {
    val today = Calendar.getInstance().getTime()
    return timeFormat.format(today)
  }
}
