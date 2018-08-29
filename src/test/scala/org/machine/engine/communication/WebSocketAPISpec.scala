/*
This test suite is to verify that the AsyncAPI specification 
for the Engine Command websocket API is specified correctly. 
Hopefully, the Scala community will eventually produce tools
that make this test suite redundant.
*/

package org.machine.engine.communication
import org.scalatest._

class WebSocketAPISpec extends FunSpecLike
  with Matchers
  with BeforeAndAfterAll{

  override def beforeAll(){
  
  }

  override def afterAll(){
    
  }

  describe("Verify Websocket API Specification"){
    it ("load the file"){
      import cats.syntax.either._
      import io.circe._
      import io.circe.generic.auto._
      import io.circe.yaml
      import io.circe.yaml.parser
      import java.io.InputStreamReader;
      import scala.io.Source;
      import java.io.File

      //Note: The cats library also defines a getClass, hence the "this" inclusion.
      val inputStream = this.getClass.getClassLoader.getResourceAsStream("asyncapi/commands.yaml")
      val json: Either[ParsingFailure, Json] = parser.parse(new InputStreamReader(inputStream))
      println(json)
    }
  }
}
