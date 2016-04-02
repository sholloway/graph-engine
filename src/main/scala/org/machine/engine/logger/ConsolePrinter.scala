package org.machine.engine.logger

class ConsolePrinter extends Printer{
  def println(message: String):Unit = {
    Console.println(message)
  }
}
