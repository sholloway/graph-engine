package org.machine.engine.graph.internal

import org.neo4j.graphdb._
import org.machine.engine.graph._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.logger._
import org.machine.engine.exceptions._

import scala.collection.JavaConversions._

object SystemSpaceManager{
  import Neo4JHelper._

  val loadSystemSpace = "match (ss:internal_system_space) return ss.mid as id, ss.name as name"
  val createSystemSpace = "merge (ss:internal_system_space {mid:{mid}, name:{name}}) return ss.mid as id, ss.name as name"

  def verifySystemSpace(db: GraphDatabaseService, logger: Logger):SystemSpace = {
    logger.debug("Engine: Verifying System Space")
    val systemSpaceResults = query[SystemSpace](db, loadSystemSpace, null, SystemSpace.queryMapper)
    var systemSpace:SystemSpace = null
    if(systemSpaceResults.isEmpty){
      logger.warn("Engine: System Space does not exist.")
      systemSpace = createSystemSpace(db, logger)
    }else{
      logger.debug("Engine: System Space was loaded.")
      systemSpace = systemSpaceResults(0)
    }
    return systemSpace
  }

  private def createSystemSpace(db: GraphDatabaseService, logger: Logger):SystemSpace = {
    logger.debug("Engine: Attempting to create System Space.")
    createUniqueSystemSpaceConstraint(db, logger)
    return insertNewSystemSpace(db, logger)
  }

  private def createUniqueSystemSpaceConstraint(db: GraphDatabaseService,
    logger: Logger) = {
    logger.debug("Engine: Attempting to create unique System Space constraint.")
    transaction(db, (graphDB:GraphDatabaseService)=>{
      graphDB
        .schema()
        .constraintFor(InternalElementsLabels.internal_system_space)
        .assertPropertyIsUnique( "name" )
        .create();
    })
  }

  private def insertNewSystemSpace(db: GraphDatabaseService,
    logger: Logger):SystemSpace = {
      logger.debug("Engine: Attempting to insert new System Space node.")
      val createSystemSpaceParams = Map("mid"->uuid, "name"->"System Space")
      var systemSpaces:Array[SystemSpace] = null
      transaction(db, (graphDB: GraphDatabaseService) =>{
        systemSpaces = run[SystemSpace](graphDB, createSystemSpace, createSystemSpaceParams, SystemSpace.queryMapper)
      })

      if (systemSpaces.isEmpty){
        logger.critical("Engine: System Space could not be created.")
        throw new InternalErrorException("Could not create the system space.")
      }
      logger.debug("Engine: System Space was provisioned.")
      return systemSpaces(0)
  }
}
