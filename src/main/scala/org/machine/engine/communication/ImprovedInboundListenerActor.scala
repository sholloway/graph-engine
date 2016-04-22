package org.machine.engine.communication

import akka.actor.{Actor, PoisonPill}
import akka.actor.Actor.Receive
import akka.actor.Props

import org.zeromq.ZMQ
import org.zeromq.ZMQ.{Context, Socket}
import org.zeromq.ZMQException
import zmq.ZError

import scala.concurrent.duration._
import scala.collection.JavaConversions._

case class CheckForMessages()
case class ShutDown()
case class Idle()

class ImprovedInboundListenerActor extends Actor{
  import context._
  implicit val systemRef = context.system
  var zeroMQContext: Context = null
  var socket: Socket = null
  var pollingDuration: FiniteDuration = null

  override def preStart(){
    system.log.debug("preStart")
    sys.addShutdownHook(shutdownHook)
    determinePollingDuration
    zeroMQContext = ZMQ.context(zmq.ZMQ.ZMQ_IO_THREADS)
    socket = zeroMQContext.socket(ZMQ.REP)
    val path = determineSocketPath
    try{
      socket.bind(path)
    }catch{
      case e: org.zeromq.ZMQException => {
        system.log.error(s"Failed to bind to $path")
        system.log.error(s"Error code: ${e.getErrorCode}")
        system.log.error(s"Error code Meaning: ${ZError.toString(e.getErrorCode)}")
        system.log.error(s"Error Message: ${e.getMessage}")
        context.stop(self)
      }
    }
  }

  override def postStop(){
    system.log.debug("shuttingDown")
    socket.close
    zeroMQContext.term
  }

  private def shutdownHook = {
    systemRef.log.info("running shutdown hook")
    self ! PoisonPill
  }

  private def determineSocketPath:String = {
    val port = system.settings.config.getInt("engine.communication.inbound.port")
    val transport = system.settings.config.getString("engine.communication.inbound.transport")
    return s"$transport://*:$port"
  }

  private def determinePollingDuration:FiniteDuration = {
    val pollingInterval: Int = system.settings.config.getInt("engine.communication.inbound.polling-interval")
    return FiniteDuration(pollingInterval, MILLISECONDS)
  }

  /*
  The idea is that I don't want to wait if there are messages coming in.
  So if there is data, keep looping otherwise, wait.

  Right now, the second socket recv is never finding any data.
  How about we check for 100 milliseconds then give up?

  TODO: I need to check for Thread.Interrupt. If that happens I need to shut down gracefully.
  */
  def uglyCheckForMessages:Unit = {
    system.log.debug("checkForMessages")
    // var inboundRequest = socket.recv(zmq.ZMQ.ZMQ_DONTWAIT)
    var inboundRequest = poll()
    while(inboundRequest != null){
      system.log.debug("Found a message!")
    //  val message = new String(inboundRequest)
      val message = new String(inboundRequest, 0, inboundRequest.length)
      // val message = new String(inboundRequest, "UTF-8")
      // system.log.debug(message)
      val reply = "pong".getBytes
      socket.send(reply, 0) //So this doesn't finish and goes straight to the next line...
      inboundRequest = poll()
      // inboundRequest = socket.recv(zmq.ZMQ.ZMQ_DONTWAIT) //This is currently faster than the sender.
      // inboundRequest = socket.recv(0) //This will loop internally and blocks. Don't do this.
    }
    system.scheduler.scheduleOnce(pollingDuration, self, CheckForMessages)
    // system.scheduler.scheduleOnce(100 milliseconds, self, CheckForMessages)
  }

  //Got to get the typing of the byte[] resolved.
  // Could I do a lazy val rather than a null var?
  private def poll():Array[Byte] = {
    val deadline = 1.second.fromNow
    var dataFound = false
    var message = null.asInstanceOf[Array[Byte]]
    while(deadline.hasTimeLeft || !dataFound){
      message = socket.recv(zmq.ZMQ.ZMQ_DONTWAIT)
      dataFound = (message == null)
    }
    if (message == null){
      null.asInstanceOf[Array[Byte]]
    }else{
      return message
    }
  }

  private def processInboundMessages:Unit = {
    val inboundRequest = socket.recv(zmq.ZMQ.ZMQ_DONTWAIT)
    if(inboundRequest != null){
      val message = new String(inboundRequest, 0, inboundRequest.length)
      system.log.debug(message)
      val reply = "pong".getBytes
      socket.send(reply, 0)
    }
  }

  def receive = {
    case CheckForMessages => {
      // system.log.debug("checkForMessages")
      processInboundMessages
      self ! CheckForMessages
    }
    case ShutDown => context.stop(self)
  }
}
