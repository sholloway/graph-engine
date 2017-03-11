package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.nodes._

class CreateNewUser(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions)extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():InsertCmdResult[String] = {
    logger.debug("CreateNewUser: Executing Command")
    generateId(cmdOptions)

    //TODO: Need to enforce all the required bits.
    transaction(database, (graphDB:GraphDatabaseService) => {
      createUser(graphDB)
      registerUser(graphDB)
    })
    val mid = cmdOptions.option[String]("mid")
    return InsertCmdResult(mid.toString)
  }

  private def createUser(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateNewUser: Creating a new user.")
    cmdOptions.addOption("creationTime", time)
    val createUserStatement = """
      |create(u:user
      |{
      |  mid: {mid},
      |  first_name:{firstName},
      |  last_name:{lastName},
      |  email_address:{emailAddress},
      |  user_name: {userName},
      |  creation_time: {creationTime}
      |})
      """.stripMargin

    run( graphDB,
      createUserStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[User])
  }

  private def registerUser(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateNewUser: Associating the user with system space.")
    val stmt = """
    |match (ss:label)
    |match(u:user) where u.mid = {mid}
    |merge (ss)-[:registered]->(u)
    """.stripMargin.replaceAll("label", cmdScope.scope)
    run(graphDB,
      stmt,
      cmdOptions.toJavaMap,
      emptyResultProcessor[User])
  }
}
