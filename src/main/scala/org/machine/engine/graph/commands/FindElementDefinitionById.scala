package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class FindElementDefinitionById(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends FindElementDefinition with LazyLogging{
  import Neo4JHelper._

  def execute():QueryCmdResult[ElementDefinition] = {
    logger.debug("FindElementDefinitionById: Executing Command")
    val findElement = buildQuery(cmdScope, cmdOptions)
    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findElement,
      cmdOptions.toJavaMap,
      elementDefAndPropDefQueryMapper)
    val elementDefs = consolidateElementDefs(records.toList)
    return QueryCmdResult(validateQueryResponse(elementDefs));
  }

  /*
  FIXME Only finds element definition with properties.
  This is incorrect. The DSL and WS API both allows creation of
  element definitions without properties.

  Make the compose_of an optional relationship in the query.
  */
  private def buildQuery(cmdScope: CommandScope, cmdOptions: GraphCommandOptions):String = {
    val edMatchClause = buildElementDefinitionMatchClause(cmdOptions)
    val scope = buildScope(cmdScope, cmdOptions)
    return """
      |match (ss:scope)-[:exists_in]->(ed:element_definition ed_match)
      |optional match (ed:element_definition ed_match)-[:composed_of]->(pd:property_definition)
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

  private def buildElementDefinitionMatchClause(cmdOptions: GraphCommandOptions):String = {
    return "{mid:{mid}}"
  }

  private def validateQueryResponse(elementDefs: List[ElementDefinition]):List[ElementDefinition] = {
    if(elementDefs.length < 1){
      throw new InternalErrorException(noElementDefFoundErrorMsg());
    }else if(elementDefs.length > 1){
      throw new InternalErrorException(tooManyElementDefsFoundErrorMsg());
    }
    return elementDefs
  }

  private def noElementDefFoundErrorMsg():String = {
    val id = cmdOptions.option[String]("mid")
    return cmdScope match{
      case CommandScopes.SystemSpaceScope => "No element definition with ID: %s could be found in %s".format(id, cmdScope.scope)
      case CommandScopes.UserSpaceScope => "No element definition with ID: %s could be found in %s".format(id, cmdScope.scope)
      case CommandScopes.DataSetScope => {
        val dsIdentifier = getDataSetIdentifier(cmdOptions)
        "No element definition with ID: %s could be found in dataset: %s".format(id, dsIdentifier)
      }
    }
  }

  private def tooManyElementDefsFoundErrorMsg():String = {
    val id = cmdOptions.option[String]("mid")
    return cmdScope match{
      case CommandScopes.SystemSpaceScope => "Multiple Element Definitions where found with ID: %s in %s".format(id, cmdScope.scope)
      case CommandScopes.UserSpaceScope => "Multiple Element Definitions where found with ID: %s in %s".format(id, cmdScope.scope)
      case CommandScopes.DataSetScope => {
        val dsIdentifier = getDataSetIdentifier(cmdOptions)
        "Multiple Element Definitions where found with ID: %s in dataset: %s".format(id, dsIdentifier)
      }
    }
  }
}
