package org.machine.engine.logger

import java.util.Calendar
import java.text.SimpleDateFormat
class Logger(var level: LoggerLevel, var printer:Printer = new ConsolePrinter()){
  val timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS z")

  def info(msg: String) = {
    if (level <= LoggerLevels.INFO){
      log(LoggerLevels.INFO,now, msg)
    }
  }

  def debug(msg: String) = {
    if (level <= LoggerLevels.DEBUG){
      log(LoggerLevels.DEBUG,now, msg)
    }
  }

  def warn(msg: String) = {
    if (level <= LoggerLevels.WARNING){
      log(LoggerLevels.WARNING, now, msg)
    }
  }

  def error(msg: String) = {
    if (level <= LoggerLevels.ERROR){
      log(LoggerLevels.ERROR, now, msg)
    }
  }

  def critical(msg: String) = {
    if (level <= LoggerLevels.CRITICAL){
      log(LoggerLevels.CRITICAL, now, msg)
    }
  }

  def println(msg:String) = {
    printer.println(msg)
  }

  private def log(loggingLevel:LoggerLevel, time:String, msg: String) = {
    val str = s"${loggingLevel.display}: $time - $msg"
    this.println(str)
  }

  private def now():String = {
    val today = Calendar.getInstance().getTime()
    return timeFormat.format(today)
  }
}
