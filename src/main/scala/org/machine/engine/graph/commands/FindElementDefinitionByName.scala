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

class FindElementDefinitionByName(_database:GraphDatabaseService,
  _cmdScope:CommandScope,
  _commandOptions:Map[String, AnyRef],
  _logger:Logger) extends FindElementDefinition{
  protected def commandOptions:Map[String, AnyRef] = this._commandOptions
  protected def database:GraphDatabaseService = this._database
  protected def cmdScope:CommandScope = this._cmdScope
  protected def logger:Logger = this._logger
  
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
