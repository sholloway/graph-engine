package org.machine.engine.communication

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.actor.{Actor, Props}

import scala.concurrent.duration._

class AkkaHTTPSpike extends TestKit(ActorSystem("AkkaHTTPSpike")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{

  // override def beforeAll(){}

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
  }

  /*
  This is a test designed to start the web server. It is not designed to be ran
  as part of the CI build.
  */
  describe("Recieving HTTP & WebSocket Requests"){
    it("should event on an incoming message"){
      val server = new WebServer()
      server.start()
    }
  }
}
