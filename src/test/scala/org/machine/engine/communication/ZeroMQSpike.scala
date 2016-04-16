package org.machine.engine.communication

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.actor.{Actor, Props}
import akka.pattern.gracefulStop
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

import org.zeromq.ZMQ
import org.zeromq.ZMQ.{Context, Socket}

class ZeroMQSpike extends TestKit(ActorSystem("ZeroMQSpec")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{

  override def beforeAll(){}

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
  }

  describe("0MQ Communication"){
    /*
    Get the test working with just sending ping/pong messages, then Refactor
    to use protobuf.
    */
    it ("should do stuff"){
      val listener = system.actorOf(Props[InboundListenerActor])
      listener ! "start"
      listener ! "listen"
      Client.connect
      val msgResponse = Client.sendMsg("Hi")
      msgResponse should equal("pong")
      try {
        val stopped: Future[Boolean] = gracefulStop(listener, 5 seconds, "stop")
        Await.result(stopped, 6 seconds)
        // the actor has been stopped
      } catch {
        case e: akka.pattern.AskTimeoutException => fail("the actor wasn't stopped within 5 seconds")
      }
    }
  }
}

object Client{
  var context: Context = null
  var socket: Socket = null
  val port:Int = 5150 //TODO: this needs to be in the configuration file and not hard coded.
  val transport:String = "tcp"
  val host = "localhost"

  def connect: Unit = {
    Console.println("Client.connect")
    context = ZMQ.context(zmq.ZMQ.ZMQ_IO_THREADS)
    socket = context.socket(ZMQ.REQ)
    socket.connect(s"$transport://$host:$port")
  }

  def sendMsg(msg: String):String = {
    Console.println("Client.sendMsg")
    if (socket == null){ // TODO: I would like to verify that the connection is established.
      connect
    }

    val communicationAttempt = socket.send(msg.getBytes, 0)
    if(!communicationAttempt){
        return "Failed to send the message."
    }

    val reply = socket.recv(0)
    return new String(reply,0,reply.length)
  }
}
