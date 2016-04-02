package org.machine.engine.logger

class StringPrinter extends Printer{
  val buffer = new StringBuffer()
  def println(message: String):Unit = {
    buffer.append(message)
    buffer.append("\n")
  }

  def log():String = {
    return buffer.toString
  }
}
