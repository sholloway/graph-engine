package org.machine.engine.communication

import akka.actor.{Actor, PoisonPill}
import akka.actor.Actor.Receive
import akka.actor.Props
import scala.concurrent.duration._

case class StartSpinning()

class SmartShutDownActor extends Actor{
  import context._
  //Reference to the ActorSystem for the
  //secondary threads.
  implicit val systemRef = context.system
  
  override def preStart(){
    system.log.info("running preStart")
    sys.addShutdownHook{
      /*
      Note: The ActorSystem is being created as an implicit in the test.
      That is why this new thread is able to access it.
      */
      systemRef.log.info("running shutdown hook")
      self ! PoisonPill
    }
  }

  override def postStop(){
    system.log.info("running postStop")
  }

  def receive = {
    // system.log.info("running receive")
    case StartSpinning => spin
  }

  private def spin = {
    system.scheduler.schedule(0.milliseconds, 300.milliseconds)(cryForHelp)
  }

  /*Using implicit reference to the ActorSystem.*/
  private def cryForHelp():Unit = {
    systemRef.log.info("Somebody stop me!")
    // Console.println("Somebody stop me!")
  }
}
