package org.machine.engine.graph.commands

import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.machine.engine.logger._
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class FindElementDefinitionByName(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends FindElementDefinition{
  import Neo4JHelper._
  
  def execute():List[ElementDefinition] = {
    logger.debug("FindElementDefinitionByName: Executing Command")
    val findElement = buildQuery(cmdScope, commandOptions)
    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findElement,
      commandOptions,
      elementDefAndPropDefQueryMapper)
    val elementDefs = consolidateElementDefs(records.toList)
    return validateQueryResponse(elementDefs);
  }

  protected def buildQuery(cmdScope:CommandScope, commandOptions:Map[String, AnyRef]):String = {
    val edMatchClause = buildElementDefinitionMatchClause(commandOptions)
    val scope = buildScope(cmdScope, commandOptions)
    return """
      |match (ss:scope)-[:exists_in]->(ed:element_definition ed_match)-[:composed_of]->(pd:property_definition)
      |return ed.mid as elementId,
      |  ed.name as elementName,
      |  ed.description as elementDescription,
      |  pd.mid as propId,
      |  pd.name as propName,
      |  pd.type as propType,
      |  pd.description as propDescription
      """.stripMargin
        .replaceAll("scope", cmdScope.scope)
        .replaceAll("ed_match", edMatchClause)
  }

  private def buildScope(cmdScope:CommandScope, options:Map[String, AnyRef]):String = {
    val scope = cmdScope match{
      case CommandScopes.SystemSpaceScope => CommandScopes.SystemSpaceScope.scope
      case CommandScopes.UserSpaceScope => CommandScopes.UserSpaceScope.scope
      case CommandScopes.DataSetScope => { "%s {name:%s}".format(CommandScopes.DataSetScope.scope, options.get("dsName"))}
    }
    return scope
  }

  protected def buildElementDefinitionMatchClause(commandOptions:Map[String, AnyRef]):String = {
    return "{name:{name}}"
  }

  protected def validateQueryResponse(elementDefs: List[ElementDefinition]):List[ElementDefinition] = {
    val name = commandOptions.get("name").getOrElse(throw new InternalErrorException("FindElementDefinitionByName requires that name be specified on commandOptions."))
    if(elementDefs.length < 1){
      val msg = "No element with Name: %s could be found in %s".format(name, cmdScope.scope)
      throw new InternalErrorException(msg);
    }else if(elementDefs.length > 1){
      val msg = "Multiple Element Definitions where found with Name: %s in %s".format(name, cmdScope.scope)
      throw new InternalErrorException(msg);
    }
    return elementDefs
  }
}
