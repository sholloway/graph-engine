package org.machine.engine.logger

import org.scalatest._
import org.scalatest.mock._
import org.machine.engine.logger._

class LoggerSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfter{
  var logger = null.asInstanceOf[Logger]
  val debugMsg = "Trying to figure something out here."
  val infoMsg = "Hey, look at this."
  val warnMsg = "Uh oh. I didn't expect that."
  val errorMsg = "Well, that was unfortunate."
  val criticalMsg = "Holy critical bug Batman!"

  before{
    logger = new Logger(LoggerLevels.DEBUG, new StringPrinter())
  }

  def logStuff(logger:Logger){
    logger.debug(debugMsg)
    logger.info(infoMsg)
    logger.warn(warnMsg)
    logger.error(errorMsg)
    logger.critical(criticalMsg)
  }

  describe("The Logger"){
    it("should log to the console by default"){
      val logger = new Logger(LoggerLevels.DEBUG)
      logger.printer.isInstanceOf[ConsolePrinter] should equal(true)
    }

    it("should log debug messages"){
      logger.level = LoggerLevels.DEBUG
      logStuff(logger)
      logger.printer.asInstanceOf[StringPrinter].log should include(debugMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(infoMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(warnMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(errorMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(criticalMsg)
    }

    it("should log info messages"){
      logger.level = LoggerLevels.INFO
      logStuff(logger)
      logger.printer.asInstanceOf[StringPrinter].log should not include(debugMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(infoMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(warnMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(errorMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(criticalMsg)
    }

    it("should log warn messages"){
      logger.level = LoggerLevels.WARNING
      logStuff(logger)
      logger.printer.asInstanceOf[StringPrinter].log should not include(debugMsg)
      logger.printer.asInstanceOf[StringPrinter].log should not include(infoMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(warnMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(errorMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(criticalMsg)
    }

    it("should log error messages"){
      logger.level = LoggerLevels.ERROR
      logStuff(logger)
      logger.printer.asInstanceOf[StringPrinter].log should not include(debugMsg)
      logger.printer.asInstanceOf[StringPrinter].log should not include(infoMsg)
      logger.printer.asInstanceOf[StringPrinter].log should not include(warnMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(errorMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(criticalMsg)
    }

    it("should log critical messages"){
      logger.level = LoggerLevels.CRITICAL
      logStuff(logger)
      logger.printer.asInstanceOf[StringPrinter].log should not include(debugMsg)
      logger.printer.asInstanceOf[StringPrinter].log should not include(infoMsg)
      logger.printer.asInstanceOf[StringPrinter].log should not include(warnMsg)
      logger.printer.asInstanceOf[StringPrinter].log should not include(errorMsg)
      logger.printer.asInstanceOf[StringPrinter].log should include(criticalMsg)
    }

    it ("should log adhoc messages"){
      val msg = "This is a message dumped to the log."
      logger.println(msg)
      logger.printer.asInstanceOf[StringPrinter].log should include(msg)
    }
  }
}
