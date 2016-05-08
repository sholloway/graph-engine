package org.machine.engine.communication

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message,BinaryMessage, TextMessage, UpgradeToWebSocket}
import akka.stream.{ActorMaterializer, Graph, FlowShape}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.io.StdIn
import scala.collection.JavaConversions._

/*
TODO:
- Rename to something self documenting.
- Wrap HTTPResponse 400 with an error function.
- Add scaladoc
- Remove the nested matches. Each supported path should map to a unique handler.
  i.e. def start()

Questions:
- What is the right way to handle protocol buffers here?
  - Should I use a Graph and map handlers?
  - Should I off load the work to another actor?
  - Should I bubble up some of the data to make pivoting in the code easier?
    - e.g. ActionType, ScopeType, EntityType, ProcessType

Next Steps:
1. Solve the basic problem of how does the server send the client a message.
  - Write a RunnableGraph that sends two messages back to the server.
    i. UUID
    ii. UUID + Message
2. Read all of the Akka Streams Docs.
3. Implement the basic flow for JSON.
  - Build out def inboundStreamGraph()
  - I'm starting to think that the core of engine will simply be
    a Stream graph. If I understand this right, I won't need to define routers
    and most of my actors. Rather the Akka Stream API will encapsulate that.
    The core logic will take a functional approach of working with
    Source, Sink, Flow, RunnableGraph elements.
  - One stream approach might be (Using Stream Graph API):
    WS Request  -> Generate UUID  ~> Unmarshal Request    -> Validate Request  ~> Select/Build Command -> Execute Command -> Build Response -> Send Response to Client w/ UUID.
                                  ~> Send UUID to client                       ~> Handle Error
3. Refactor to use protobuf.

Current Resources
http://www.smartjava.org/content/create-reactive-websocket-server-akka-streams
http://www.smartjava.org/content/backpressure-action-websockets-and-akka-streams
https://stackoverflow.com/questions/36242183/how-to-implement-a-simple-tcp-protocol-using-akka-streams
https://stackoverflow.com/questions/35120082/how-to-get-started-with-akka-streams

##Cheatsheet###################################################################
Stream
    An active process that involves moving and transforming data.
Element
    An element is the processing unit of streams. All operations transform and
    transfer elements from upstream to downstream. Buffer sizes are always
    expressed as number of elements independently form the actual size of
    the elements.
Back-pressure
    A means of flow-control, a way for consumers of data to notify a producer
    about their current availability, effectively slowing down the upstream
    producer to match their consumption speeds. In the context of Akka Streams
    back-pressure is always understood as non-blocking and asynchronous.
Non-Blocking
    Means that a certain operation does not hinder the progress of the calling
    thread, even if it takes long time to finish the requested operation.
Graph
    A description of a stream processing topology, defining the pathways through
    which elements shall flow when the stream is running.
Processing Stage
    The common name for all building blocks that build up a Graph. Examples of
    a processing stage would be operations like map(), filter(), custom
    GraphStage s and graph junctions like Merge or Broadcast.

Core abstractions:
Source
    A processing stage with exactly one output, emitting data elements whenever
    downstream processing stages are ready to receive them.
Sink
    A processing stage with exactly one input, requesting and accepting data
    elements possibly slowing down the upstream producer of elements
Flow
    A processing stage which has exactly one input and output, which connects
    its up- and downstreams by transforming the data elements flowing through it.
RunnableGraph
    A Flow that has both ends "attached" to a Source and Sink respectively, and
    is ready to be run().
*/
class WebServer {
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap.
  private implicit val executionContext = system.dispatcher

  private val httpGetMsg = """
  |<html>
  | <body>
  | This is a private channel for engine communication.
  | </body>
  |</html>""".stripMargin

  private val config = system.settings.config
  //All the subprotocols supported. Listed in descending priority.
  private val validSubprotocols:List[String] = config.getStringList("engine.communication.webserver.subprotocols").toList

  def start() = {
    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(GET, Uri.Path("/"), _, _, _) => handleRootRequest()
      case HttpRequest(GET, Uri.Path("/info"), _, _, _) => handleInfoRequest()
      case req @ HttpRequest(GET, Uri.Path("/ws"),_, _, _) => handleWebSocketRequest(req)
      case req @ HttpRequest(GET, Uri.Path("/ws/ping"),_, _, _) => handleWebSocketRequest(req)
      case _: HttpRequest =>
        HttpResponse(404, entity = "Unknown resource!")
    }

    val host = config.getString("engine.communication.webserver.host")
    val port = config.getInt("engine.communication.webserver.port")
    val bindingFuture = Http().bindAndHandleSync(requestHandler, host, port)
    println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }

  private def handleRootRequest(): HttpResponse = {
    return HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,httpGetMsg))
  }

  private def handleInfoRequest(): HttpResponse = {
    val msg = s"""
    |<html>
    | <body>
    |   <h1>Engine</h1>
    |   <hr/>
    |   Version: ${config.getString("engine.version")}
    | </body>
    |</html>
    """.stripMargin
    return HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,msg))
  }

  private def handleWebSocketRequest(req: HttpRequest):HttpResponse = {
    system.log.debug("handleWebSocketRequest")
    req.header[UpgradeToWebSocket] match {
      case Some(upgradeReqHeader) => attemptToUpgradeConnectToWS(req, upgradeReqHeader)
      case None => HttpResponse(400, entity = "Not a valid websocket request.")
    }
  }

  /*
  This function does not return immediately if the connection is established.
  */
  private def attemptToUpgradeConnectToWS(req: HttpRequest,
    upgradeReqHeader: UpgradeToWebSocket):HttpResponse = {
    system.log.info("Received request to upgrade to websocket.")

    if(upgradeReqHeader.requestedProtocols.length < 1){
      system.log.error("No subprotocol was provided by the client.")
      return HttpResponse(400, entity = "A valid subprotocol must be specified.")
    }else if(!validProtocolSpecified(upgradeReqHeader.requestedProtocols)){
      system.log.error("Unsupported protocol provided.")
      return HttpResponse(400, entity = "Unsupport subprotocol requested.")
    }
    val topLevelProtocolOption = selectTopProtocol(upgradeReqHeader.requestedProtocols)
    system.log.info("All good. Attempting to handle incoming messages.")

    val relativePath = req.uri.toRelative.toString
    relativePath match {
      case "/ws" => upgradeReqHeader.handleMessages(inboundStreamGraph(), topLevelProtocolOption)
      case "/ws/ping" => upgradeReqHeader.handleMessages(echo, topLevelProtocolOption)
    }

  }

  private def validProtocolSpecified(clientProtocols: Seq[String]):Boolean = {
    system.log.info("Validating Subprotocol")
    return clientProtocols.intersect(validSubprotocols).length > 0
  }

  private def selectTopProtocol(clientProtocols: Seq[String]):Option[String] = {
    system.log.info("Attempting to select top subprotocol.")
    val protocolOption = validSubprotocols.collectFirst{
      case protocol if clientProtocols.contains(protocol) => protocol
    }
    system.log.info(s"Subprotocol selected: ${protocolOption.get}")
    return protocolOption
  }

  /*
  TODO:
  - Make aware of the subprotocol selection.
  - Build out to work with the Google protocol buffers.
  */
  private def inboundStreamGraph(): Graph[FlowShape[Message, Message], Any] = {
    system.log.info("Building Stream Graph")
    import org.machine.engine.graph.Neo4JHelper
    return Flow[Message]
      .mapConcat {
        // we match but don't actually consume the text message here,
        // rather we simply stream it back as the tail of the response
        // this means we might start sending the response even before the
        // end of the incoming message has been received
        //Neo4JHelper.uuid
        case tm: TextMessage => {
          //Return a new UUID. This is a reciept that the message was received.
          TextMessage(Source.single(Neo4JHelper.uuid)) :: Nil //Same as List[TextMessage](TextMessage(Source.single(Neo4JHelper.uuid)))
        }
        case bm: BinaryMessage => {
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil //The response is an empty list...
        }
      }
   }

   //Simply return the message received.
   private def echo:Graph[FlowShape[Message, Message], Any] = Flow[Message]
}
