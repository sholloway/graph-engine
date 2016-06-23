package org.machine.engine.graph.commands.elementdefinition

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

class CreateElementDefintion2(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():InsertCmdResult[String] = {
    logger.debug("CreateElementDefintion: Executing Command")
    generateId(cmdOptions)
    /*
    FIXME Merge of an ED needs to consider the scope.
    Rather than using the pattern of creating a node, then Associating
    it with a scope item, the initial merge needs to consider that.
    (scope)-[:contains]->(ed)

    1. Determine scope
    2. In the scope, does the element already exist? Duplicate ED's are not allowed so a merge should be done.
    3. If the ed does not exist in the scope, create it.
    4. Create PD's and associate it.
    */
    // val workflow = Function.chain(Seq(
    //   mIdGuard,
    //   determineScope,
    //   createElementDefinition,
    //   createPropertyDefinitions,
    //   processResponse
    // ))
    //
    // transaction(database, (graphDB: GraphDatabaseService) => {
    //   workflow(graphDB, cmdScope, cmdOptions)
    // })

    //TODO This should return somethine else when an error occurs.
    return InsertCmdResult(cmdOptions.option[String]("mid"))
  }
}
