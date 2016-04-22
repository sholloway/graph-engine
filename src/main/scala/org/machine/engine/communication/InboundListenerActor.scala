package org.machine.engine.communication

import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.actor.Props

import org.zeromq.ZMQ
import org.zeromq.ZMQ.{Context, Socket}

class InboundListenerActor extends Actor{
  import context._
  var zeroMQContext: Context = null
  var socket: Socket = null
  val port:Int = 5150 //#TODO:100 this needs to be in the configuration file and not hard coded.
  val transport:String = "tcp"

  override def preStart(){
    zeroMQContext = ZMQ.context(zmq.ZMQ.ZMQ_IO_THREADS)
    socket = zeroMQContext.socket(ZMQ.REP)
    socket.bind(s"$transport://*:$port")
  }

  override def postStop(){
    socket.close
    zeroMQContext.term
  }

  def paused: Receive = {
    case "start" => become(active)
    case "stop" => context.stop(self)
  }

  def active: Receive = {
    case "pause" => become(paused)
    case "stop" => context.stop(self)
    case "listen" => {
      processInboundMessages
      self ! "listen"
    }
  }

  //start in a paused state.
  def receive = paused

  private def processInboundMessages:Unit = {
    val inboundRequest = socket.recv(0) // This loops internally until it gets a message.
    val message = new String(inboundRequest, 0, inboundRequest.length)
    system.log.debug(message)
    val reply = "pong".getBytes
    socket.send(reply, 0)
  }
}
