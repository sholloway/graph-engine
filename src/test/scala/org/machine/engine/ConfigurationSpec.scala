package org.machine.engine

import org.scalatest._
import org.scalatest.mock._
import com.typesafe.config._

class ConfigurationSpec extends FunSpec with Matchers{
  val config = ConfigFactory.load() //should load application.conf

  it ("should load application.conf"){
     config.getString("engine.version") should equal("0.1.0")
     config.getString("engine.communication.webserver.host") should equal("localhost")
  }

  it("should load the test/resources/reference.conf"){
    config.getString("engine.graphdb.path") should equal("target/TestDB.graph")
  }
}
