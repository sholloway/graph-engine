package org.machine.engine.graph

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils
import java.nio.file.{Paths, Files}
import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}

import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.logger._

class EngineDatabaseNotValidException(message: String = null, cause: Throwable = null) extends Exception
class InternalErrorException(message: String = null, cause: Throwable = null) extends Exception

object EngineQueries{
  val LoadSystemSpace = "match (ss:internal_system_space) return ss.mid as id, ss.name as name"
  val CreateSystemSpace = "merge (ss:internal_system_space {mid:{mid}, name:{name}}) return ss.mid as id, ss.name as name"
}

class Engine(dbPath:String, config: {
  val logger: Logger
}){
  import Neo4JHelper._
  import EngineQueries._

  var graphDBOption: Option[GraphDatabaseService] = None
  var systemSpaceOption:Option[SystemSpace] = None

  def systemSpace:SystemSpace = this.systemSpaceOption.getOrElse(throw new InternalErrorException("SystemSpace has not be initialized."))

  setup

  private def setup(){
    config.logger.debug("Engine: Setting Up")
    verifyFile(dbPath)
    initializeDatabase(dbPath)
    verifySystemSpace
  }

  def shutdown(){
    config.logger.debug("Engine: Shutting Down")
    //TODO: Register the Neo4J shutdown with the JVM shutdown like in the example.
    // graphDBOption.foreach(graphDB => graphDB.shutdown())
  }

  def database:GraphDatabaseService = {
    return graphDBOption.getOrElse(throw new InternalErrorException("The GraphDatabaseService was not initialized."))
  }

  private def verifyFile(filePath:String) = {
    config.logger.debug("Engine: Verifying File")
    if(!filePath.endsWith("graph")){
      config.logger.warn("The engine database file should have the 'graph' suffix.")
    }

    if(!Files.exists(Paths.get(filePath))){
      config.logger.warn("Engine: Could not find the engine database file.")
      // val errorMsg = "The engine database could not be found at: %s".format(filePath)
      // throw new EngineDatabaseNotValidException(errorMsg);
      config.logger.warn("Engine: Attempting to create a new engine database file.")
    }
  }

  private def initializeDatabase(dbPath: String) = {
    config.logger.debug("Engine: Initializing Database")
    val dbFile = new File(dbPath)
    val graphDBFactory = new GraphDatabaseFactory()
    val graphDB = graphDBFactory.newEmbeddedDatabase(dbFile)
    graphDBOption = Some(graphDB)
  }

  private def verifySystemSpace():Engine = {
    config.logger.debug("Engine: Verifying System Space")
    val db = graphDBOption.getOrElse(throw new InternalErrorException("The GraphDatabaseService was not initialized."))
    val systemSpaceResults = query[SystemSpace](db, LoadSystemSpace, null, SystemSpace.queryMapper)
    if(systemSpaceResults.isEmpty){
      config.logger.warn("Engine: System Space does not exist.")
      createSystemSpace(db)
    }else{
      config.logger.debug("Engine: System Space was loaded.")
      setSystemSpace(systemSpaceResults(0))
    }
    return this
  }

  private def createSystemSpace(db: GraphDatabaseService) = {
    config.logger.debug("Engine: Attempting to create System Space.")
    transaction(db, (graphDB:GraphDatabaseService)=>{
      graphDB
        .schema()
        .constraintFor(InternalElementsLabels.internal_system_space)
        .assertPropertyIsUnique( "name" )
        .create();
    })

    val createSystemSpaceParams = Map("mid"->uuid, "name"->"System Space")
    var systemSpaces:Array[SystemSpace] = null
    transaction(db, (graphDB: GraphDatabaseService) =>{
      systemSpaces = insert[SystemSpace](graphDB, CreateSystemSpace, createSystemSpaceParams, SystemSpace.queryMapper)
    })

    if (systemSpaces.isEmpty){
      config.logger.critical("Engine: System Space could not be created.")
      throw new InternalErrorException("Could not create the system space.")
    }else{
      config.logger.debug("Engine: System Space was provisioned.")
      setSystemSpace(systemSpaces(0))
    }
  }

  private def setSystemSpace(ss:SystemSpace):SystemSpace = {
    this.systemSpaceOption = Some(ss)
    return this.systemSpaceOption.get
  }
}
