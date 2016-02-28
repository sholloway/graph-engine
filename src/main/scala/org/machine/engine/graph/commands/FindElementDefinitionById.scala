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

class FindElementDefinitionById(_database:GraphDatabaseService,
  _cmdScope:CommandScope,
  _commandOptions:Map[String, AnyRef],
  _logger:Logger) extends FindElementDefinition{
  protected def commandOptions:Map[String, AnyRef] = this._commandOptions
  protected def database:GraphDatabaseService = this._database
  protected def cmdScope:CommandScope = this._cmdScope
  protected def logger:Logger = this._logger

  protected def buildElementDefinitionMatchClause(commandOptions:Map[String, AnyRef]):String = {
    return "{mid:{mid}}"
  }

  protected def validateQueryResponse(elementDefs: List[ElementDefinition]):List[ElementDefinition] = {
    val id = commandOptions.get("mid").getOrElse(throw new InternalErrorException("FindElementDefinitionById requires that mid be specified on commandOptions."))
    if(elementDefs.length < 1){
      val msg = "No element with ID: %s could be found in %s".format(id, cmdScope.scope)
      throw new InternalErrorException(msg);
    }else if(elementDefs.length > 1){
      val msg = "Multiple Element Definitions where found with ID: %s in %s".format(id, cmdScope.scope)
      throw new InternalErrorException(msg);
    }
    return elementDefs
  }
}
