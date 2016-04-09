package org.machine.engine

import org.scalatest._
import org.scalatest.mock._
import com.typesafe.config._

class ConfigurationSpec extends FunSpec with Matchers{
  it ("should load application.conf"){
     val config = ConfigFactory.load() //should load application.conf
     config.getString("foo.bar") should equal("blah")
  }
}
