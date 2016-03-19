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

class FindElementDefinitionByName(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends FindElementDefinition{
  import Neo4JHelper._

  def execute():List[ElementDefinition] = {
    logger.debug("FindElementDefinitionByName: Executing Command")
    val findElement = buildQuery(cmdScope, cmdOptions)
    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findElement,
      cmdOptions.toJavaMap,
      elementDefAndPropDefQueryMapper)
    val elementDefs = consolidateElementDefs(records.toList)
    return validateQueryResponse(elementDefs);
  }

  protected def buildQuery(cmdScope: CommandScope, cmdOptions: GraphCommandOptions):String = {
    val edMatchClause = buildElementDefinitionMatchClause(cmdOptions)
    val scope = buildScope(cmdScope, cmdOptions)
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

  protected def buildElementDefinitionMatchClause(cmdOptions: GraphCommandOptions):String = {
    return "{name:{name}}"
  }

  protected def validateQueryResponse(elementDefs: List[ElementDefinition]):List[ElementDefinition] = {
    if(elementDefs.length < 1){
      throw new InternalErrorException(noElementDefFoundErrorMsg());
    }else if(elementDefs.length > 1){
      throw new InternalErrorException(tooManyElementDefsFoundErrorMsg());
    }
    return elementDefs
  }

  private def noElementDefFoundErrorMsg():String = {
    val name = cmdOptions.option[String]("name")
    return cmdScope match{
      case CommandScopes.SystemSpaceScope => "No element with Name: %s could be found in %s".format(name, cmdScope.scope)
      case CommandScopes.UserSpaceScope => "No element with Name: %s could be found in %s".format(name, cmdScope.scope)
      case CommandScopes.DataSetScope => {
        val dsIdentifier = getDataSetIdentifier(cmdOptions)
        "No element with Name: %s could be found in dataset: %s".format(name, dsIdentifier)
      }
    }
  }

  private def tooManyElementDefsFoundErrorMsg():String = {
    val name = cmdOptions.option[String]("name")
    return cmdScope match{
      case CommandScopes.SystemSpaceScope => "Multiple Element Definitions where found with Name: %s in %s".format(name, cmdScope.scope)
      case CommandScopes.UserSpaceScope => "Multiple Element Definitions where found with Name: %s in %s".format(name, cmdScope.scope)
      case CommandScopes.DataSetScope => {
        val dsIdentifier = getDataSetIdentifier(cmdOptions)
        "Multiple Element Definitions where found with Name: %s in dataset: %s".format(name, dsIdentifier)
      }
    }
  }
}
