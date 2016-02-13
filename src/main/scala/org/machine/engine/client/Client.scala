package org.machine.engine.client

import org.zeromq.ZMQ
import org.zeromq.ZMQ.{Context,Socket}

/*
Will be replaced with a unit test.
Posts "ping" to the InboundCimpohannel server.
*/
object Client{
  def talk = {
    val context = ZMQ.context(1)
    val socket = context.socket(ZMQ.REQ) //client socket
    println("Connecting to server…")
    socket.connect ("tcp://localhost:5555")
    for(count <- 1 to 10){
      val request = "ping".getBytes()
      println(request)
      socket.send(request, 0)

      //get the reply
      val reply = socket.recv(0)
      println("Recieved "+new String(reply,0,reply.length))
    }
    socket.send("stop".getBytes(), 0)
  }
}
