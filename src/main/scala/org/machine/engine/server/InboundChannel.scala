package org.machine.engine.server

import org.zeromq.ZMQ
import org.zeromq.ZMQ.{Context, Socket}

/*
Creates a bound socket on port 5555.
Terminates when recieves a "stop" message.
Responds to the client with a "pong" message. (ping/pong)
*/
object InboundChannel{
  def listen() = {
    val context = ZMQ.context(1) //Magic number...

    // request/response socket
    val socket = context.socket(ZMQ.REP)
    println("Starting up the channel.")
    socket.bind("tcp://*:5555") //seperate out the transport and port.

    var happy = true;
    while(happy){
      val inboundRequest = socket.recv(0) //Magic number...
      val message = new String(inboundRequest, 0, inboundRequest.length)
      println("Recieved: "+ message)

      try{
        Thread.sleep(1000) //Magic number
      }catch{
        case e: InterruptedException => e.printStackTrace()
      }
      val reply = "pong".getBytes
      //reply(reply.length-1)=0 //Sets the last byte of the reply to 0
      socket.send(reply, 0)

      if(message == "stop"){
        socket.close();
        context.term();
        happy = false;
      }
    }
  }
}
