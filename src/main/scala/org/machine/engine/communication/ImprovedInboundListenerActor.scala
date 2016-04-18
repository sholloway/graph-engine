package org.machine.engine.communication

import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.actor.Props

import org.zeromq.ZMQ
import org.zeromq.ZMQ.{Context, Socket}
import scala.concurrent.duration._

case class CheckForMessages()
case class ShutDown()
case class Idle()

class ImprovedInboundListenerActor extends Actor{
  import context._
  var zeroMQContext: Context = null
  var socket: Socket = null

  override def preStart(){
    system.log.debug("preStart")
    val port:Int = system.settings.config.getInt("engine.communication.inbound.port")
    val transport:String = system.settings.config.getString("engine.communication.inbound.transport")
    zeroMQContext = ZMQ.context(zmq.ZMQ.ZMQ_IO_THREADS)
    socket = zeroMQContext.socket(ZMQ.REP)
    socket.bind(s"$transport://*:$port")
    super.preStart()
  }

  override def postStop(){
    system.log.debug("All done")
    super.postStop()
  }

  def idle: Receive = {
    case CheckForMessages => checkForMessages
    case ShutDown => shuttingDown
  }

  /*
  The idea is that I don't want to wait if there are messages coming in.
  So if there is data, keep looping otherwise, wait.
  */
  def checkForMessages:Unit = {
    system.log.debug("checkForMessages")
    //Is there a way to test if a message exists without calling socket.recv?
    var inboundRequest = socket.recv(zmq.ZMQ.ZMQ_DONTWAIT) // TODO: Magic number...
    while(inboundRequest != null){
      system.log.debug("Found a message!")
      val message = new String(inboundRequest, 0, inboundRequest.length)
      system.log.debug(message)
      val reply = "pong".getBytes
      socket.send(reply, 0)
      inboundRequest = socket.recv(zmq.ZMQ.ZMQ_DONTWAIT) //Attempt to fetch another message.
    }
    //There are no more messages, so let's wait a little while before checking again.
    //At first glance, I don't know if there's a way to get out of this loop.
    //Perhaps if I create a handler for this and call cancel on it.
    //
    system.scheduler.scheduleOnce(100 milliseconds, self, CheckForMessages)
  }

  def shuttingDown:Unit = {
    system.log.debug("shuttingDown")
    socket.close
    zeroMQContext.term
    context.stop(self)
  }

  def receive = idle
}
