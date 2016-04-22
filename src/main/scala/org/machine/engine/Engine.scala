package org.machine.engine

import java.io.File;
import java.io.IOException;
import java.nio.file.{Paths, Files}

import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

// import scala.language.reflectiveCalls
import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import com.typesafe.scalalogging._


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class Engine(dbPath:String) extends GraphDSL with LazyLogging{
  import Neo4JHelper._
  import SystemSpaceManager._
  import UserSpaceManager._

  var graphDBOption: Option[GraphDatabaseService] = None
  var systemSpaceOption:Option[SystemSpace] = None
  var userSpaceOption:Option[UserSpace] = None
  var scope:CommandScope = CommandScopes.SystemSpaceScope
  var command:EngineCommand = EngineCommands.DefineElement
  val cmdOptions:GraphCommandOptions = new GraphCommandOptions()

  def systemSpace:SystemSpace = this.systemSpaceOption.getOrElse(throw new InternalErrorException("SystemSpace has not be initialized."))
  def userSpace:UserSpace = this.userSpaceOption.getOrElse(throw new InternalErrorException("UserSpace has not be initialized."))

  setup

  private def setup(){
    logger.debug("Engine: Setting Up")
    verifyFile(dbPath)
    initializeDatabase(dbPath)
    setSystemSpace(verifySystemSpace(database))
    setUserSpace(verifyUserSpace(database))
  }

  def shutdown(){
    logger.debug("Engine: Shutting Down")
    //#TODO:70 Register the Neo4J shutdown with the JVM shutdown like in the example.
    database.shutdown()
  }

  def database:GraphDatabaseService = {
    return graphDBOption.getOrElse(throw new InternalErrorException("The GraphDatabaseService was not initialized."))
  }

  private def verifyFile(filePath:String) = {
    logger.debug("Engine: Verifying File")
    if(!filePath.endsWith("graph")){
      logger.warn("The engine database file should have the 'graph' suffix.")
    }

    if(!Files.exists(Paths.get(filePath))){
      logger.warn("Engine: Could not find the engine database file.")
      logger.warn("Engine: Attempting to create a new engine database file.")
    }
  }

  private def initializeDatabase(dbPath: String) = {
    logger.debug("Engine: Initializing Database")
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
    logger.debug("Engine: Set command scope to system space.")
    this.scope = CommandScopes.SystemSpaceScope
    return this
  }

  def inUserSpace():GraphDSL = {
    logger.debug("Engine: Set command scope to user space.")
    this.scope = CommandScopes.UserSpaceScope
    return this
  }

  def createDataSet(name:String, description:String):String = {
    this.scope = CommandScopes.UserSpaceScope
    command = EngineCommands.CreateDataSet
    cmdOptions.reset
    cmdOptions.addOption("mid", uuid)
    cmdOptions.addOption("name", name)
    cmdOptions.addOption("description", description)
    val cmd = CommandFactory.build(command, database, scope, cmdOptions)
    return cmd.execute()
  }

  def datasets():List[DataSet] = {
    this.scope = CommandScopes.UserSpaceScope
    cmdOptions.reset
    val cmd = new ListDataSets(database, scope, cmdOptions)
    return cmd.execute()
  }

  def findDataSetByName(name:String):DataSet = {
    this.scope = CommandScopes.UserSpaceScope
    cmdOptions.reset
    cmdOptions.addOption("name", name)
    val cmd = new FindDataSetByName(database, scope, cmdOptions)
    val elements = cmd.execute()
    return elements(0)
  }

  def onDataSet(id: String):GraphDSL = {
    scope = CommandScopes.DataSetScope
    command = EngineCommands.EditDataSet
    cmdOptions.reset
    cmdOptions.addOption("dsId", id)
    return this;
  }

  def onDataSetByName(name: String):GraphDSL = {
    scope = CommandScopes.DataSetScope
    command = EngineCommands.EditDataSet
    cmdOptions.reset
    cmdOptions.addOption("dsName", name)
    return this;
  }

  def findDataSetById(id: String):DataSet = {
    scope = CommandScopes.UserSpaceScope
    cmdOptions.reset
    cmdOptions.addOption("dsId", id)
    val cmd = new FindDataSetById(database, scope, cmdOptions)
    val elements = cmd.execute()
    return elements(0)
  }

  //Resets the command options and sets the command type to Define Element.
  def defineElement(name:String, description: String):GraphDSL = {
    logger.debug("Engine: Define Element")
    if(scope != CommandScopes.DataSetScope){
      cmdOptions.reset
    }
    cmdOptions.addOption("mid",uuid)
    cmdOptions.addOption("name",name)
    cmdOptions.addOption("description",description)
    cmdOptions.addOption("properties", new PropertyDefinitions())
    command = EngineCommands.DefineElement
    return this
  }

  def withProperty(name:String, ptype: String, description: String):GraphDSL = {
    logger.debug("Engine: With property name:%s ptype:%s".format(name, ptype))
    val propertyDef = new PropertyDefinition(uuid, name, ptype, description)
    val props = cmdOptions.option[PropertyDefinitions]("properties")
    props.addProperty(propertyDef)
    return this
  }

  def elementDefinitions():List[ElementDefinition] = {
    val cmd = new ListAllElementDefinitions(database, scope, cmdOptions)
    return cmd.execute()
  }

  def findElementDefinitionById(id:String):ElementDefinition = {
    if(scope != CommandScopes.DataSetScope){
      cmdOptions.reset
    }
    cmdOptions.addOption("mid", id)
    val cmd = new FindElementDefinitionById(database, scope, cmdOptions)
    val elements = cmd.execute()
    return elements(0)
  }

  def findElementDefinitionByName(name:String):ElementDefinition = {
    if(scope != CommandScopes.DataSetScope){
      cmdOptions.reset
    }
    cmdOptions.addOption("name", name)
    val cmd = new FindElementDefinitionByName(database, scope, cmdOptions)
    val elements = cmd.execute()
    return elements(0)
  }

  /** Sets the mode to be in edit for an ElementDefintion.
      Resets Command Options to be focused on editing an ElementDefinition.
    @param id: The id for the ElementDefinition.
  */
  def onElementDefinition(id: String):GraphDSL = {
    command = EngineCommands.EditElementDefinition
    if(scope != CommandScopes.DataSetScope){
      cmdOptions.reset
    }
    cmdOptions.addOption("mid", id)
    return this
  }

  /** Sets the description on an ElementDefinition.
    @param description: The description of the ElementDefinition.
  */
  def setDescription(description: String):GraphDSL = {
    cmdOptions.addOption("description", description)
    return this
  }

  /** Sets the name on an ElementDefinition.
    @param name: The name of the ElementDefinition.
  */
  def setName(name:String):GraphDSL = {
    cmdOptions.addOption("name", name)
    return this
  }

  /** Sets the name on an ElementDefinition.
    @param type: The strong type of the property.
  */
  def setType(name:String):GraphDSL = {
    cmdOptions.addOption("type", name)
    return this
  }

  def editPropertyDefinition(name:String):GraphDSL = {
    command = EngineCommands.EditElementPropertyDefinition
    cmdOptions.addOption("pname", name)
    return this
  }

  def removePropertyDefinition(name: String):GraphDSL = {
    command = EngineCommands.RemoveElementPropertyDefinition
    cmdOptions.addOption("pname",name)
    return this
  }

  def delete():GraphDSL = {
    command = command match {
      case EngineCommands.EditElementDefinition => EngineCommands.DeleteElementDefintion
      case EngineCommands.EditElement => EngineCommands.DeleteElement
      case EngineCommands.EditAssociation => EngineCommands.DeleteAssociation
      case unknown => throw new InternalErrorException("Cannot delete when %s is selected".format(unknown.toString()))
    }
    return this
  }
  /** Executes the built up command. */
  def end():String = {
    logger.debug("Engine: Attempt to execute command.")
    val cmd = CommandFactory.build(command,
      database,
      scope,
      cmdOptions)
    return cmd.execute()
  }

  def provision(elementDefId: String):GraphDSL = {
    command = EngineCommands.ProvisionElement
    cmdOptions.addOption("edId", elementDefId)
    cmdOptions.addOption("mid", uuid)
    return this
  }

  def withField(fieldName: String, fieldValue:Any):GraphDSL = {
    cmdOptions.addField(fieldName, fieldValue)
    return this
  }

  def findElement(elementId: String):Element = {
    cmdOptions.addOption("mid", elementId)
    return new FindElementById(database, scope, cmdOptions).execute()
  }

  def onElement(elementId: String):GraphDSL = {
    command = EngineCommands.EditElement
    cmdOptions.addOption("elementId", elementId)
    return this
  }

  def setField(fieldName: String, fieldValue: Any):GraphDSL ={
    return withField(fieldName, fieldValue)
  }

  def attach(startingElementId: String):GraphDSL = {
    command = EngineCommands.AssociateElements
    cmdOptions.addOption("startingElementId", startingElementId)
    cmdOptions.addOption("associationId", uuid)
    return this
  }

  def to(endingElementId: String):GraphDSL = {
    cmdOptions.addOption("endingElementId", endingElementId)
    return this
  }

  def as(associationName: String):GraphDSL = {
    cmdOptions.addOption("associationName", associationName)
    return this
  }

  def findAssociation(associationId: String):Association = {
    cmdOptions.addOption("associationId", associationId)
    return new FindAssociationById(database, scope, cmdOptions).execute()
  }

  def onAssociation(annotationId: String):GraphDSL = {
    command = EngineCommands.EditAssociation
    cmdOptions.addOption("associationId", annotationId)
    return this
  }

  def removeField(fieldName: String):GraphDSL={
    cmdOptions.addOption(fieldName, fieldName)
    command = command match {
      case EngineCommands.EditElement => EngineCommands.RemoveElementField
      case EngineCommands.RemoveElementField => EngineCommands.RemoveElementField
      case EngineCommands.EditAssociation => EngineCommands.RemoveAssociationField
      case EngineCommands.RemoveAssociationField => EngineCommands.RemoveAssociationField
      case unknown => throw new InternalErrorException("Cannot remove fields when %s is selected".format(unknown.toString()))
    }
    return this
  }

  def findOutboundAssociations():List[Association] = {
    return new FindOutboundAssociationsByElementId(database, scope, cmdOptions).execute()
  }

  def findInboundAssociations():List[Association] = {
    return new FindInboundAssociationsByElementId(database, scope, cmdOptions).execute()
  }

  def findDownStreamElements():List[Element] = {
    return new FindDownStreamElementsByElementId(database, scope, cmdOptions).execute()
  }

  def findUpStreamElements():List[Element] = {
    return new FindUpStreamElementsByElementId(database, scope, cmdOptions).execute()
  }

  def removeInboundAssociations():List[Association] = {
    val existingInboundAssociations = new FindInboundAssociationsByElementId(database, scope, cmdOptions).execute()
    val ids = ArrayBuffer.empty[String]
    existingInboundAssociations.foreach(a => ids += a.id)
    new RemoveInboundAssociations(database, scope, cmdOptions, ids.toList).execute()
    return existingInboundAssociations
  }

  def removeOutboundAssociations():List[Association] = {
    val existingOutboundAssociations = new FindOutboundAssociationsByElementId(database, scope, cmdOptions).execute()
    val ids = ArrayBuffer.empty[String]
    existingOutboundAssociations.foreach(a => ids += a.id)
    new RemoveOutboundAssociations(database, scope, cmdOptions, ids.toList).execute()
    return existingOutboundAssociations
  }
}
