package org.machine.engine.communication

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.actor.{Actor, Props}

import scala.concurrent.duration._

class SmartShutDownActorSpec extends TestKit(ActorSystem("SmartShutDownActorSpec")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{

  // override def beforeAll(){}

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
  }

  /*
  This test is designed to prove out how an actor can gracefully handle the
  scenario where the user sends an SIGINT by Ctrl+C. It is not designed to be
  ran as part of the build.
  */
  describe("Handling SIGINT in an Actor"){
    ignore("should handle SIGINT (Ctrl+C)"){
      val badActor = system.actorOf(Props[SmartShutDownActor],"SmartShutDownActor")
      Console.println("Interrupt the test by mannually killing the JVM with Ctrl+C")
      badActor ! StartSpinning
      //Interrupt the test by mannually killing the JVM with Ctrl+C
      Thread.sleep(10000)
    }
  }
}
