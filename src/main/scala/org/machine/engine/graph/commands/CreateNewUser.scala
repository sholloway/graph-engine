package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import java.util.Random;
import org.neo4j.graphdb._

import org.machine.engine.authentication.PasswordTools
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.nodes._


class CreateNewUser(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions)extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._
  import PasswordTools._;

  val SALT_BYTE_SIZE = 64 // 512 bits
  val PBKDF2_ITERATIONS = 20000

  def execute():InsertCmdResult[String] = {
    logger.debug("CreateNewUser: Executing Command")
    generateId(cmdOptions)

    //TODO: Need to enforce all the required bits.
    transaction(database, (graphDB:GraphDatabaseService) => {
      createUser(graphDB)
      createUserCredential(graphDB)
      associateUserCredential(graphDB)
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

  private def createUserCredential(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateNewUser: Creating a new user's credential.")

    val seed:Long = generateSeed()
    val generator:Random = createRandomNumberGenerator(seed)
    val salt:Array[Byte] = generateSalt(generator, SALT_BYTE_SIZE)
    val passwordHash:Array[Byte] = generateHash(cmdOptions.option[String]("userPassword"), salt, PBKDF2_ITERATIONS)
    val pwdHashStr:String = hashToBase64(passwordHash)

    cmdOptions.addOption("credId", uuid)
    cmdOptions.addOption("passwordHash", pwdHashStr)
    cmdOptions.addOption("passwordSalt", SALT_BYTE_SIZE)
    cmdOptions.addOption("hashIterationCount", PBKDF2_ITERATIONS)

    val createUserCredentialStatement = """
      |create(uc:credential
      |{
      |  mid: {credId},
      |  password_hash: {passwordHash},
      |  password_salt: {passwordSalt},
      |  hash_iteration_count: {hashIterationCount},
      |  creation_time: {creationTime},
      |  last_modified_time: {creationTime}
      |})
      """.stripMargin

    run( graphDB,
      createUserCredentialStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[User])

    //Remove the password from memory immediately.
    cmdOptions.addOption("userPassword", "REDACTED")
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

  private def associateUserCredential(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateNewUser: Associating the user credential with the user.")
    val stmt = """
    |match(u:user) where u.mid = {mid}
    |match (uc:credential)
    |merge (u)-[:authenticates_with]->(uc)
    """.stripMargin
    run(graphDB,
      stmt,
      cmdOptions.toJavaMap,
      emptyResultProcessor[User])
  }
}
