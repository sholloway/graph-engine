package org.machine.engine.communication

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
// import akka.actor.{Actor, Props}

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, RunnableGraph, Source, Sink}

import org.machine.engine.graph.Neo4JHelper

class AkkaStreamSpike extends TestKit(ActorSystem("AkkaStreamSpike")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{

  // override def beforeAll(){}

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
  }

  /*
  This is a test designed to start the web server. It is not designed to be ran
  as part of the CI build.
  */
  describe("Working with streams"){
    it("it should use a sink with a source"){
      // implicit val system = ActorSystem("Sys")
      implicit val materializer = ActorMaterializer()
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      val source = Source(List(1,2,3,4,5))
      val sink = Sink.seq[Int] //try to collect everything.
      val runnable: RunnableGraph[Future[Seq[Int]]] = source.toMat(sink)(Keep.right)
      val result: Future[Seq[Int]] = runnable.run()
      result.onSuccess{
        case r => {
          r should have length(5)
        }
      }
    }

    /*
    I want to be able to add data to the source. (i.e. a UUID)
    The output of the flow should be an envelope data structure containing
    the original input and the enriched data.
    The Sink should have a collection of all of the inputs.
    */
    it ("should use a flow to enrich the source"){
      implicit val materializer = ActorMaterializer()
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      case class Enrichment(original: Int, id: String)
      val source = Source(List(1,2,3,4,5))
      val sink = Sink.seq[Enrichment]

      val process = (item: Int) => {
        Enrichment(item, Neo4JHelper.uuid)
      }

      val flow:Flow[Int, Enrichment, _] = Flow.fromFunction(process)
      val runnable: RunnableGraph[Future[Seq[Enrichment]]] = source.via(flow).toMat(sink)(Keep.right)
      val result: Future[Seq[Enrichment]] = runnable.run()

      result.onSuccess{
        case r => {
          r should have length(5)
          //r.foreach(i => Console.println(i))
        }
      }
    }

    /*
    I need two things to occure.
    1) UUID sent back per request.
    2) Another message with UUID sent back after a random delay.

    source ~> enrich UUID ~> broadcast ~> process.async
                                       ~> return UUID

    The uuid should always be returned first so perhaps:
    source ~> enrich UUID ~> return UUID ~> process.async

    Possibly use the secondary publisher approach detailed in:
    http://www.smartjava.org/content/create-reactive-websocket-server-akka-streams

    Attemp to merge in secondary messages with Merge trait.
    Client ~> Enrich ~> Merge ~> output
              Source2 ~> Merge ~> output
    */
    it("should return enriched results and from publisher"){
      implicit val materializer = ActorMaterializer()
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      case class Enrichment(original: Int, id: String){
        override def toString():String = {
          s"id: $original uuid: $id"
        }
      }

      val clientSource = Source(List(1,2,3,4,5,6,7,8,9))
      val sink = Sink.seq[Enrichment]

      val process = (item: Int) => {
        Enrichment(item, Neo4JHelper.uuid)
      }

      // val enrich:Flow[Int, Enrichment, _] = Flow.fromFunction(process).named("enrichmentFlow")

      val graph = GraphDSL.create(){ implicit builder: GraphDSL.Builder[akka.NotUsed] =>
        import GraphDSL.Implicits._
        val anotherSource = Source(List(17,22,1772, 234, 19))
        val merge = builder.add(Merge[Int](2)) //MergePreferred
        val enrich:FlowShape[Int, Enrichment]= builder.add(Flow.fromFunction(process))

        anotherSource ~> merge.in(1)
                         merge ~> enrich

        FlowShape(merge.in(0), enrich.out)
      }.named("partialGraph")

      val workflow = Flow.fromGraph(graph)

      val runnable: RunnableGraph[Future[Seq[Enrichment]]] = clientSource.via(workflow).toMat(sink)(Keep.right)
      val result: Future[Seq[Enrichment]] = runnable.run()

      result.onSuccess{
        case r => {
          r should have length(14)
          //r.foreach(i => Console.println(i))
        }
      }
    }

  }
}
