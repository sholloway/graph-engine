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
      // import io.circe.parser._
      import io.circe.generic.auto._
      import io.circe.yaml
      import io.circe.yaml.parser
      import java.io.InputStreamReader;
      import scala.io.Source;
      import java.io.File

      //Note: The cats library also defines a getClass, hence the "this" inclusion.
      val inputStream = this.getClass.getClassLoader.getResourceAsStream("asyncapi/commands.yaml")
      val jsonEither: Either[ParsingFailure, Json] = parser.parse(new InputStreamReader(inputStream))
      println(jsonEither)

      val json:Json = jsonEither
        .leftMap((error) => { 
          println(s"An error occured parsing the API spec. ${error}")
          fail()
        })
        .right //This gives us a RightProjection
        .get //Unwraps the RightProject
        
      val specVersionOption: Option[String] = json.hcursor.get[String]("asyncapi").toOption
      specVersionOption.get should equal("1.2.0")
     
      val apiVersionOption: Option[String] = json.hcursor
        .downField("info")
        .get[String]("version").toOption
      apiVersionOption.get should equal("0.1.0")
      
      val receiveEvents: Vector[Json] = json.hcursor
        .downField("events")
        .downField("receive")
        .focus
        .flatMap(_.asArray)
        .getOrElse(Vector.empty)
      receiveEvents.length should equal(1)      
      
      val sendEvents: Vector[Json] = json.hcursor
        .downField("events")
        .downField("send")
        .focus
        .flatMap(_.asArray)
        .getOrElse(Vector.empty)
      sendEvents.length should equal(1)      

      

      /* Next steps
      1. Use a before to parse the YAML once.
      2. Split the above into multiple tests.
      3. Design the rest of the Layout API
      4. Evaluate if it's worth trying to do this at run time 
         to verify the APIs requests and respones. 
         
      Note: 
      If I do use this at runtime, need to only parse the YAML upon 
      engine start up. Will need to evaluate migrating from Lift to Circe 
      for JSON duties. No point in having multiple JSON frameworks if 
      it can be avoided.

      I do feel like the whole AsyncAPI thing is a rabbit hole 
      that is distracting me from getting the core app working.
      On the flip side, documenting the API needs to happen
      and could potentially help simplify building the Cockpit.
      */
    }
  }
}
