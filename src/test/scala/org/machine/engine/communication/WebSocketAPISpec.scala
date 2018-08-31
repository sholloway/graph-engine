/*
This test suite is to verify that the AsyncAPI specification 
for the Engine Command websocket API is specified correctly. 
Hopefully, the Scala community will eventually produce tools
that make this test suite redundant.
*/

package org.machine.engine.communication
import org.scalatest._

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.yaml
import io.circe.yaml.parser
import java.io.InputStreamReader;
import scala.io.Source;
import java.io.File

import com.typesafe.config._

class WebSocketAPISpec extends FunSpecLike
  with Matchers
  with BeforeAndAfterAll{

  val config = ConfigFactory.load()
  var json:Json = null
  override def beforeAll(){
    //Note: The cats library also defines a getClass, hence the "this" inclusion.
    val inputStream = this.getClass.getClassLoader.getResourceAsStream("asyncapi/commands.yaml")
    val jsonEither: Either[ParsingFailure, Json] = parser.parse(new InputStreamReader(inputStream))
    // println(jsonEither)

    json = jsonEither
      .leftMap((error) => { 
        println(s"An error occured parsing the API spec. ${error}")
        fail()
      })
      .right //This gives us a RightProjection
      .get //Unwraps the RightProject
  }

  describe("Verify Websocket API Specification"){
    it ("should use version 1.2.0 of AsyncAPI spec"){
      json.hcursor
        .get[String]("asyncapi")
        .toOption
        .get should equal("1.2.0")
    }

    it ("should identify what version the API is at"){
      val apiVersion = config.getString("engine.version")
      json.hcursor
        .downField("info")
        .get[String]("version")
        .toOption
        .get should equal(apiVersion)
    }

    it ("should declare all the inbound messages the service can receive"){
      val receiveEvents: Vector[Json] = json.hcursor
        .downField("events")
        .downField("receive")
        .focus
        .flatMap(_.asArray)
        .getOrElse(Vector.empty)
      receiveEvents.length should equal(1)      
    }

    it ("should declare all the inbound messages the service can send"){
      val sendEvents: Vector[Json] = json.hcursor
        .downField("events")
        .downField("send")
        .focus
        .flatMap(_.asArray)
        .getOrElse(Vector.empty)
      sendEvents.length should equal(1)      
    }
  }
}
