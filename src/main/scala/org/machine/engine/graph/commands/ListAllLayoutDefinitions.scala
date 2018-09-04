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

class ListAllLayoutDefinitions(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4JQueryCommand[LayoutDefinition] with LazyLogging{
  import Neo4JHelper._

  def execute():QueryCmdResult[LayoutDefinition] = {
    logger.debug("ListAllLayoutDefintions: Executing Command")
    val listLayoutDefsStmt = """
      |match (ss:internal_system_space)-[:exists_in]->(ld:layout_definition)
      |return ld.mid as id,
      |  ld.name as name,
      |  ld.description as description,
      |  ld.creation_time as creationTime,
      |  ld.last_modified_time as lastModifiedTime
      """.stripMargin

    val records:Array[LayoutDefinition] = query[LayoutDefinition](database,
      listLayoutDefsStmt, 
      cmdOptions.toJavaMap,
      layoutDefQueryMapper)
    return QueryCmdResult(records.toList)
  }

  private def layoutDefQueryMapper(
    results: ArrayBuffer[LayoutDefinition],
    record: java.util.Map[java.lang.String, Object]) = {
    results += mapLayoutDefintion(record)
  }

  private def mapLayoutDefintion(record: java.util.Map[java.lang.String, Object]):LayoutDefinition = {
    val id = record.get("id").toString()
    val name = record.get("name").toString()
    val description = record.get("description").toString()
    val creationTime = record.get("creationTime").toString()
    val lastModifiedTime = record.get("lastModifiedTime").toString()
    return new LayoutDefinition(id, name, description, creationTime, lastModifiedTime)
  }
}
