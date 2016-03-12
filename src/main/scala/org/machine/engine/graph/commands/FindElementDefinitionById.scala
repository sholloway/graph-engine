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

class FindElementDefinitionById(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends FindElementDefinition{
  import Neo4JHelper._

  def execute():List[ElementDefinition] = {
    logger.debug("FindElementDefinitionById: Executing Command")
    val findElement = buildQuery(cmdScope, commandOptions)
    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findElement,
      commandOptions,
      elementDefAndPropDefQueryMapper)
    val elementDefs = consolidateElementDefs(records.toList)
    return validateQueryResponse(elementDefs);
  }

  private def buildQuery(cmdScope:CommandScope, commandOptions:Map[String, AnyRef]):String = {
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
        .replaceAll("scope", scope)
        .replaceAll("ed_match", edMatchClause)
  }

  private def buildElementDefinitionMatchClause(commandOptions:Map[String, AnyRef]):String = {
    return "{mid:{mid}}"
  }

  private def validateQueryResponse(elementDefs: List[ElementDefinition]):List[ElementDefinition] = {
    if(elementDefs.length < 1){
      val msg = noElementDefFoundErrorMsg()
      throw new InternalErrorException(msg);
    }else if(elementDefs.length > 1){
      val msg = tooManyElementDefsFoundErrorMsg()
      throw new InternalErrorException(msg);
    }
    return elementDefs
  }

  private def noElementDefFoundErrorMsg():String = {
    val id = commandOptions.get("mid").getOrElse(throw new InternalErrorException("FindElementDefinitionById requires that mid be specified on commandOptions."))
    return cmdScope match{
      case CommandScopes.SystemSpaceScope => "No element with ID: %s could be found in %s".format(id, cmdScope.scope)
      case CommandScopes.UserSpaceScope => "No element with ID: %s could be found in %s".format(id, cmdScope.scope)
      case CommandScopes.DataSetScope => {
        val dsIdentifier = getDataSetIdentifier(commandOptions)
        "No element with ID: %s could be found in dataset: %s".format(id, dsIdentifier)
      }
    }
  }

  private def tooManyElementDefsFoundErrorMsg():String = {
    val id = commandOptions.get("mid").getOrElse(throw new InternalErrorException("FindElementDefinitionById requires that mid be specified on commandOptions."))
    return cmdScope match{
      case CommandScopes.SystemSpaceScope => "Multiple Element Definitions where found with ID: %s in %s".format(id, cmdScope.scope)
      case CommandScopes.UserSpaceScope => "Multiple Element Definitions where found with ID: %s in %s".format(id, cmdScope.scope)
      case CommandScopes.DataSetScope => {
        val dsIdentifier = getDataSetIdentifier(commandOptions)
        "Multiple Element Definitions where found with ID: %s in dataset: %s".format(id, dsIdentifier)
      }
    }
  }
}
