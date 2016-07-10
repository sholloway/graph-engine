package org.machine.engine

import org.scalatest._
import org.scalatest.mock._
import org.scalatest.concurrent.ScalaFutures

//https://stackoverflow.com/questions/17623728/how-to-pass-configuration-file-to-scala-jar-file
class WebServerUserSpaceSpec extends FunSpecLike
  with Matchers with ScalaFutures with BeforeAndAfterAll{
  describe("Main"){
    //perhaps rather than this, I could change the existing integration tests
    //to call the main.
  }
}
