package org.machine.engine

import com.typesafe.config._
import com.typesafe.scalalogging.{LazyLogging}

import java.io.File;
import java.io.IOException;
import java.nio.file.{Paths, Files}

import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.utilities._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.decisions._

import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

object Engine{
  val EmptyResultErrorMsg = "Empty Result"

  private val config = ConfigFactory.load()
  private val dbPath = config.getString("engine.graphdb.path")
  private implicit var engine:Option[Engine] = None

  def getInstance: Engine = {
    if(engine == None){
      val decisionTree = DecisionDSL.buildDecisionTreeFromRules()
      // Console.println("########################################################")
      // Console.println("Printing the Engine Decision Tree")
      // DecisionDSL.drawTree(decisionTree,0, new ConsolePlotter())
      // Console.println("########################################################")
      engine = Some(new Engine(dbPath, decisionTree))
      sys.addShutdownHook(shutdown)
    }
    engine.get
  }

  /*
  TODO Make private to force singleton use.
  */
  private def shutdown = {
    Console.println("Neo4J Shutting Down")
    engine.foreach(_.shutdown())
    engine = None
  }

  def databasePath = dbPath
}

class Engine private (dbPath:String, decisionTree: Question) extends GraphDSL with LazyLogging{
  import Neo4JHelper._
  import SystemSpaceManager._
  import UserSpaceManager._

  var graphDBOption: Option[GraphDatabaseService] = None
  var systemSpaceOption:Option[SystemSpace] = None
  var userSpaceOption:Option[UserSpace] = None
  var scope:CommandScope = CommandScopes.SystemSpaceScope
  var command:EngineCommand = EngineCommands.DefineElement
  var cmdOptions:GraphCommandOptions = new GraphCommandOptions()
  var actionType:ActionType = ActionTypes.None
  var user:Option[String] = None
  var entityType:EntityType = EntityTypes.None
  var filter:Filter = Filters.None

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
    import org.neo4j.logging.slf4j.Slf4jLogProvider
    import org.neo4j.graphdb.factory.GraphDatabaseSettings

    val graphDB = graphDBFactory
      .setUserLogProvider(new Slf4jLogProvider())
      .newEmbeddedDatabaseBuilder(dbFile)
      .setConfig( GraphDatabaseSettings.dump_configuration, "true" )
      .newGraphDatabase()

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

  //////////////////////////////////////////////////////////////////////////////
  //Abstract handlers
  def reset():GraphDSL = {
    cmdOptions.reset
    actionType = ActionTypes.None
    user = None
    entityType = EntityTypes.None
    filter = Filters.None
    return this
  }

  def setUser(user: Option[String]):GraphDSL = {
    this.user = user
    return this;
  }

  def setScope(scope: CommandScope):GraphDSL = {
    this.scope = scope
    return this
  }

  def setActionType(actionType: ActionType):GraphDSL = {
    this.actionType = actionType
    return this
  }

  def setEntityType(entityType: EntityType):GraphDSL = {
    this.entityType = entityType
    return this
  }

  def setFilter(filter: Filter):GraphDSL = {
    this.filter = filter
    return this
  }

  def setOptions(options: GraphCommandOptions):GraphDSL = {
    cmdOptions = options
    return this
  }

  /*
  BUG
  The current issue is that Userspace doesn't make sense any more. It needs to
  be replaced with the user vertex.
  Action Items:
  1. Modify this run command to throw an error if there is no active User ID.
  2. Modify this run command to inject the active user ID into the cmdOptions.
  3. Update all Neo4J creation statements to leverage the user ID.
  4. Update all Neo4J update statements to leverage the active user ID.
  5. Update all Neo4J delete statements to leverage the active user ID.
  6. Update all websocket tests to use an active user.
  7. Update all unit tests to leverage an active user.
  */
  def run():EngineCmdResult = {
    val decisionRequest = DecisionRequest(this.user,
      this.actionType,
      this.scope,
      this.entityType,
      this.filter)

    val decision = DecisionDSL.findDecision(this.decisionTree, decisionRequest)
    val cmd = DynamicCmdLoader.provision(decision.name, database, scope, cmdOptions)
    return cmd.execute()
  }
  //End Abstract handlers
  //////////////////////////////////////////////////////////////////////////////

  def inSystemSpace():GraphDSL = {
    logger.debug("Engine: Set command scope to system space.")
    return setScope(CommandScopes.SystemSpaceScope)
  }

  def inUserSpace():GraphDSL = {
    logger.debug("Engine: Set command scope to user space.")
    return setScope(CommandScopes.UserSpaceScope)
  }

  def createDataSet(name:String, description:String):String = {
    this.scope = CommandScopes.UserSpaceScope
    command = EngineCommands.CreateDataSet
    cmdOptions.reset
    cmdOptions.addOption("mid", uuid)
    cmdOptions.addOption("name", name)
    cmdOptions.addOption("description", description)
    val cmd = CommandFactory.build(command, database, scope, cmdOptions)
    val result:EngineCmdResult = cmd.execute()
    val resultId = getResultValue(result)
    return resultId
  }

  def datasets():Seq[DataSet] = {
    this.scope = CommandScopes.UserSpaceScope
    cmdOptions.reset
    val cmd = new ListDataSets(database, scope, cmdOptions)
    val result:QueryCmdResult[DataSet] = cmd.execute()
    return result.results
  }

  def findDataSetByName(name:String):DataSet = {
    this.scope = CommandScopes.UserSpaceScope
    cmdOptions.reset
    cmdOptions.addOption("name", name)
    val cmd = new FindDataSetByName(database, scope, cmdOptions)
    val result:QueryCmdResult[DataSet] = cmd.execute()
    return result.results(0)
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
    val result:QueryCmdResult[DataSet] = cmd.execute()
    return result.results(0)
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
    cmdOptions.addOption("creationTime",time)
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

  def elementDefinitions():Seq[ElementDefinition] = {
    val cmd = new ListAllElementDefinitions(database, scope, cmdOptions)
    val result:QueryCmdResult[ElementDefinition] = cmd.execute()
    return result.results
  }

  def findElementDefinitionById(id:String):ElementDefinition = {
    if(scope != CommandScopes.DataSetScope){
      cmdOptions.reset
    }
    cmdOptions.addOption("mid", id)
    val cmd = new FindElementDefinitionById(database, scope, cmdOptions)
    val result:QueryCmdResult[ElementDefinition] = cmd.execute()
    handleErrorResult(result)
    return result.results.head
  }

  def findElementDefinitionByName(name:String):ElementDefinition = {
    if(scope != CommandScopes.DataSetScope){
      cmdOptions.reset
    }
    cmdOptions.addOption("name", name)
    val cmd = new FindElementDefinitionByName(database, scope, cmdOptions)
    val elements = cmd.execute()
    val result:QueryCmdResult[ElementDefinition] = cmd.execute()
    handleErrorResult(result)
    handleEmptyResult[ElementDefinition](result)
    return result.results.head
  }

  private def handleEmptyResult[T](result: QueryCmdResult[T]){
    if (result.results.isEmpty){
      throw new InternalErrorException(Engine.EmptyResultErrorMsg)
    }
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
  /**
      Executes the built up command.
      Only used for commands of type Insert, Update, Delete.
      No queries.

      TODO: Replace CommandFactory with Decision Tree.
      Do this after all commands have their ancestry updated.
  */
  def end():String = {
    logger.debug("Engine: Attempt to execute command.")
    val cmd = CommandFactory.build(command,
      database,
      scope,
      cmdOptions)
    val result:EngineCmdResult = cmd.execute()
    handleErrorResult(result)
    val resultId = getResultValue(result)
    return resultId
  }

  private def handleErrorResult(result: EngineCmdResult){
    if (result.status != EngineCmdResultStatuses.OK){
      result.errorMessage.foreach{ msg =>
        throw new InternalErrorException(msg)
      }
    }
  }

  private def getResultValue(result: EngineCmdResult):String = result match {
    case r: InsertCmdResult[String @unchecked] => r.result
    case r: UpdateCmdResult[String @unchecked] => r.result
    case r: DeleteCmdResult[String @unchecked] => r.result
    case _ => throw new InternalErrorException("Unsupported EngineCmdResult type")
  }

  def provision(elementDefId: String):GraphDSL = {
    command = EngineCommands.ProvisionElement
    cmdOptions.addOption("edId", elementDefId)
    cmdOptions.addOption("mid", uuid)
    return this
  }

  def elements():Seq[Element] = {
    setActionType(ActionTypes.Retrieve)
    setFilter(Filters.All)
    setEntityType(EntityTypes.Element)
    val results:EngineCmdResult = run
    if (results.status != EngineCmdResultStatuses.OK){
      results.errorMessage.foreach(msg => throw new InternalErrorException(msg))
    }
    return results.asInstanceOf[QueryCmdResult[Element]].results
  }

  def withField(fieldName: String, fieldValue:Any):GraphDSL = {
    cmdOptions.addField(fieldName, fieldValue)
    return this
  }

  def findElement(elementId: String):Element = {
    cmdOptions.addOption("mid", elementId)
    val cmd = new FindElementById(database, scope, cmdOptions)
    val result:QueryCmdResult[Element] = cmd.execute()
    return result.results.head
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
    val cmd = new FindAssociationById(database, scope, cmdOptions)
    val result:QueryCmdResult[Association] = cmd.execute()
    return result.results.head
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

  def findOutboundAssociations():Seq[Association] = {
    val cmd = new FindOutboundAssociationsByElementId(database, scope, cmdOptions)
    val result:QueryCmdResult[Association] = cmd.execute()
    return result.results
  }

  def findInboundAssociations():Seq[Association] = {
    val cmd = new FindInboundAssociationsByElementId(database, scope, cmdOptions)
    val result:QueryCmdResult[Association] = cmd.execute()
    return result.results
  }

  def findDownStreamElements():Seq[Element] = {
    val cmd = new FindDownStreamElementsByElementId(database, scope, cmdOptions)
    val result:QueryCmdResult[Element] = cmd.execute()
    return result.results
  }

  def findUpStreamElements():Seq[Element] = {
    val cmd = new FindUpStreamElementsByElementId(database, scope, cmdOptions)
    val result:QueryCmdResult[Element] = cmd.execute()
    return result.results
  }

  def removeInboundAssociations() = {
    new RemoveInboundAssociations(database, scope, cmdOptions).execute()
  }

  def removeOutboundAssociations() = {
    new RemoveOutboundAssociations(database, scope, cmdOptions).execute()
  }

  def createUser():GraphDSL = {
    command = EngineCommands.CreateNewUser
    cmdOptions.reset
    return this.inSystemSpace()
  }

  def withFirstName(name:String):GraphDSL = {
    return withField("firstName", name)
  }

  def withLastName(name:String):GraphDSL = {
    return withField("lastName", name)
  }

  def withEmailAddress(email:String):GraphDSL = {
    return withField("emailAddress", email)
  }

  def withUserName(name:String):GraphDSL = {
    return withField("userName", name)
  }

  def withUserPassword(password:String):GraphDSL = {
    return withField("userPassword", password)
  }
}
