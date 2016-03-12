package org.machine.engine

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils
import java.nio.file.{Paths, Files}
import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.machine.engine.logger._
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class Engine(dbPath:String, config: {
  val logger: Logger
})extends GraphDSL{
  import Neo4JHelper._
  import SystemSpaceManager._
  import UserSpaceManager._

  var graphDBOption: Option[GraphDatabaseService] = None
  var systemSpaceOption:Option[SystemSpace] = None
  var userSpaceOption:Option[UserSpace] = None
  var scope:CommandScope = CommandScopes.SystemSpaceScope
  var command:EngineCommand = EngineCommands.DefineElement
  var commandOptions:Map[String, AnyRef] = Map()

  def systemSpace:SystemSpace = this.systemSpaceOption.getOrElse(throw new InternalErrorException("SystemSpace has not be initialized."))
  def userSpace:UserSpace = this.userSpaceOption.getOrElse(throw new InternalErrorException("UserSpace has not be initialized."))

  setup

  private def setup(){
    config.logger.debug("Engine: Setting Up")
    verifyFile(dbPath)
    initializeDatabase(dbPath)
    setSystemSpace(verifySystemSpace(database, config.logger))
    setUserSpace(verifyUserSpace(database, config.logger))
  }

  def shutdown(){
    config.logger.debug("Engine: Shutting Down")
    //TODO: Register the Neo4J shutdown with the JVM shutdown like in the example.
    database.shutdown()
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

  private def setSystemSpace(ss:SystemSpace):SystemSpace = {
    this.systemSpaceOption = Some(ss)
    return this.systemSpaceOption.get
  }

  private def setUserSpace(us:UserSpace):UserSpace = {
    this.userSpaceOption = Some(us)
    return this.userSpaceOption.get
  }

  def inSystemSpace():GraphDSL = {
    config.logger.debug("Engine: Set command scope to system space.")
    this.scope = CommandScopes.SystemSpaceScope
    return this
  }

  def inUserSpace():GraphDSL = {
    config.logger.debug("Engine: Set command scope to user space.")
    this.scope = CommandScopes.UserSpaceScope
    return this
  }

  def createDataSet(name:String, description:String):GraphDSL = {
    this.scope = CommandScopes.UserSpaceScope
    command = EngineCommands.CreateDataSet
    commandOptions = Map[String, AnyRef](
      "mid" -> uuid,
      "name" -> name,
      "description" -> description)
    val cmd = CommandFactory.build(command, database, scope,commandOptions, config.logger)
    cmd.execute()
    return this
  }

  def datasets():List[DataSet] = {
    this.scope = CommandScopes.UserSpaceScope
    commandOptions = Map[String, AnyRef]()
    val cmd = new ListDataSets(database, scope, commandOptions, config.logger)
    return cmd.execute()
  }

  //Resets the command options and sets the command type to Define Element.
  def defineElement(name:String, description: String):GraphDSL = {
    config.logger.debug("Engine: Define Element")
    command = EngineCommands.DefineElement
    commandOptions = Map[String, AnyRef]("mid" -> uuid,
      "name" -> name,
      "description" -> description,
      "properties" -> new ListBuffer[Map[String, Any]]())
    return this
  }

  def withProperty(name:String, ptype: String, description: String):GraphDSL = {
    config.logger.debug("Engine: With property name:%s ptype:%s".format(name, ptype))
    val propertyDef = Map[String, Any]("mid" -> uuid,
      "name" -> name,
      "type" -> ptype,
      "description" -> description)
    val props = commandOptions("properties").asInstanceOf[ListBuffer[Map[String, Any]]]
    props += propertyDef
    return this
  }

  def elements():List[ElementDefinition] = {
    val cmd = new ListAllElementDefinitions(database, scope, commandOptions, config.logger)
    return cmd.execute()
  }

  def findElementDefinitionById(id:String):ElementDefinition = {
    commandOptions = Map[String, AnyRef]("mid" -> id)
    val cmd = new FindElementDefinitionById(database, scope, commandOptions, config.logger)
    val elements = cmd.execute()
    return elements(0)
  }

  def findElementDefinitionByName(name:String):ElementDefinition = {
    commandOptions = Map[String, AnyRef]("name" -> name)    
    val cmd = new FindElementDefinitionByName(database, scope, commandOptions, config.logger)
    val elements = cmd.execute()
    return elements(0)
  }

  /** Sets the mode to be in edit for an ElementDefintion.
      Resets Command Options to be focused on editing an ElementDefinition.
    @param id: The id for the ElementDefinition.
  */
  def onElementDefinition(id: String):GraphDSL = {
    command = EngineCommands.EditElementDefinition
    commandOptions = Map[String, AnyRef]("mid" -> id)
    return this
  }

  /** Sets the description on an ElementDefinition.
    @param description: The description of the ElementDefinition.
  */
  def setDescription(description: String):GraphDSL = {
    commandOptions.+=("description"->description)
    return this
  }

  /** Sets the name on an ElementDefinition.
    @param name: The name of the ElementDefinition.
  */
  def setName(name:String):GraphDSL = {
    commandOptions.+=("name"->name)
    return this
  }

  /** Sets the name on an ElementDefinition.
    @param type: The strong type of the property.
  */
  def setType(name:String):GraphDSL = {
    commandOptions.+=("type"->name)
    return this
  }

  def editPropertyDefinition(name:String):GraphDSL = {
    command = EngineCommands.EditElementPropertyDefinition
    commandOptions.+=("pname"->name)
    return this
  }

  def removePropertyDefinition(name: String):GraphDSL = {
    command = EngineCommands.RemoveElementPropertyDefinition
    commandOptions.+=("pname"->name)
    return this
  }

  def delete():GraphDSL = {
    command = command match {
      case EngineCommands.EditElementDefinition => EngineCommands.DeleteElementDefintion
      case unknown => throw new InternalErrorException("Cannot delete when %s is selected".format(unknown.toString()))
    }
    return this
  }
  /** Executes the built up command. */
  def end():GraphDSL = {
    config.logger.debug("Engine: Attempt to execute command.")
    val cmd = CommandFactory.build(command, database, scope, commandOptions,
      config.logger)
    cmd.execute();
    return this
  }
}
