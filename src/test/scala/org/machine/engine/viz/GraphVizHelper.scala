package org.machine.engine.viz

import sys.process._
import java.io.{BufferedWriter, File, FileWriter, IOException};

import org.machine.engine.graph.Neo4JHelper

import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
// import org.neo4j.graphdb.ReturnableEvaluator;
// import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
// import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.walk.Walker;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.neo4j.visualization.graphviz.GraphvizWriter

object GraphVizHelper{
  import Neo4JHelper._

  /** Intented for debugging integration tests.
  Given a GraphDatabaseService, the funtion will generate
  a GraphViz dot file of the entire database, save the file
  to disk, generate a pdf visualization then open the image.

  TODO Leverage a chaining strategy or short circuit strategy.
  This code needs to harden to be able to gracefully handle a
  failure. Currently the calling client has no way to detect failures.
  */
  def visualize(db: GraphDatabaseService,
    dir: String ="/tmp",
    filename: String ="graphInMemory.dot") = {
    val graph = databaseToDotVizString(db)
    writefile(s"$dir/$filename", graph)
    val vizFile = generateImage(dir, filename, "pdf")
    open(dir,vizFile)
  }

  /** Generates a GraphViz representation of an entire Neo4J database.
  */
  def databaseToDotVizString(db: GraphDatabaseService):String = {
    var output: String = null
    transaction(db, (graphDB: GraphDatabaseService) => {
      val out = new ByteArrayOutputStream();
      val writer = new GraphvizWriter();
      writer.emit(out, Walker.fullGraph(graphDB))
      output = out.toString()
    })
    return output
  }

  /** Writes a string to disk.
  */
  def writefile(path: String, msg: String) = {
    var bw:Option[BufferedWriter] = None
    try{
      val file = new File(path)
      bw = Some(new BufferedWriter(new FileWriter(file)))
      bw.foreach{_.write(msg)}
    }catch{
      case e: IOException => print(e)
      case _:Throwable => println("Could not write to file.")
    }finally{
      bw.foreach{_.close()}
    }
  }

  /** Runs the GraphViz dot command on a specified dot file.
  */
  def generateImage(dotFilePath: String,
    dotFileName: String,
    outputType: String):String = {
    val cmd = s"dot $dotFileName -T$outputType -O"
    Process(cmd, new File(dotFilePath)).!!
    return s"$dotFileName.$outputType"
  }

  //consider using a callback reacting to failure
  /** Runs the Apple open command on a file.
  */
  def open(path: String, fileName: String) = {
    try{
      val cmd = s"open $fileName"
      Process(cmd, new File(path)).!!
    }catch{
      case e: RuntimeException => println(e)
    }
  }

  /** Returns the cononical working directory.
  */
  def wd:String = System.getProperty("user.dir")
}
