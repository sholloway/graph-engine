package org.machine.engine.graph.internal

import com.typesafe.scalalogging._
import org.neo4j.graphdb._
import org.machine.engine.graph._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._

import org.machine.engine.exceptions._

import scala.collection.JavaConversions._
import scala.collection.JavaConversions._

object UserSpaceManager extends StrictLogging{
  import Neo4JHelper._

  val loadUserSpace = "match (us:internal_user_space) return us.mid as id, us.name as name"
  val createUserSpace = "merge (us:internal_user_space {mid:{mid}, name:{name}}) return us.mid as id, us.name as name"

  def verifyUserSpace(db: GraphDatabaseService):UserSpace = {
    logger.debug("Engine: Verifying User Space")
    val userSpaceResults = query[UserSpace](db, loadUserSpace, null, UserSpace.queryMapper)
    var userSpace:UserSpace = null
    if(userSpaceResults.isEmpty){
      logger.warn("Engine: User Space does not exist.")
      userSpace = createUserSpace(db)
    }else{
      logger.debug("Engine: User Space was loaded.")
      userSpace = userSpaceResults(0)
    }
    return userSpace
  }

  private def createUserSpace(db: GraphDatabaseService):UserSpace = {
    logger.debug("Engine: Attempting to create User Space.")
    createUniqueUserSpaceConstraint(db)
    return insertNewUserSpace(db)
  }

  private def createUniqueUserSpaceConstraint(db: GraphDatabaseService) = {
    logger.debug("Engine: Attempting to create unique User Space constraint.")
    transaction(db, (graphDB:GraphDatabaseService)=>{
      graphDB
        .schema()
        .constraintFor(InternalElementsLabels.internal_user_space)
        .assertPropertyIsUnique( "name" )
        .create();
    })
  }

  private def insertNewUserSpace(db: GraphDatabaseService):UserSpace = {
      logger.debug("Engine: Attempting to insert new User Space node.")
      val createUserSpaceParams = Map("mid"->uuid, "name"->"User Space")
      var userSpaces:Array[UserSpace] = null
      transaction(db, (graphDB: GraphDatabaseService) =>{
        userSpaces = run[UserSpace](graphDB, createUserSpace, createUserSpaceParams, UserSpace.queryMapper)
      })

      if (userSpaces.isEmpty){
        logger.error("Engine: User Space could not be created.")
        throw new InternalErrorException("Could not create the user space.")
      }
      logger.debug("Engine: User Space was provisioned.")
      return userSpaces(0)
  }
}
