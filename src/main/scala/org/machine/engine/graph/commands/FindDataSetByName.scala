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

class FindDataSetByName(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4JQueryCommand[DataSet] with LazyLogging{
  import Neo4JHelper._

  def execute():QueryCmdResult[DataSet] = {
    logger.debug("FindDataSetByName: Executing Command")
    val findDataSets = """
      |match (u:user {mid: {activeUserId}})-[:owns]->(ds:data_set {name:{name}})
      |return ds.mid as id,
      |  ds.name as name,
      |  ds.description as description,
      |  ds.creation_time as creationTime,
      |  ds.last_modified_time as lastModifiedTime
      """.stripMargin

    val records = query[DataSet](database,
      findDataSets, cmdOptions.toJavaMap, dataSetMapper)
    return validateQueryResponse(records.toList)
  }

  private def dataSetMapper(
    results: ArrayBuffer[DataSet],
    record: java.util.Map[java.lang.String, Object]) = {
    results += mapDataSet(record)
  }

  private def mapDataSet(record: java.util.Map[java.lang.String, Object]):DataSet = {
    val id = mapString("id", record, true)
    val name = mapString("name", record, true)
    val description = mapString("description", record, true)
    val creationTime = mapString("creationTime", record, true)
    val lastModifiedTime = mapString("lastModifiedTime", record, false)
    return new DataSet(id, name, description, creationTime, lastModifiedTime)
  }

  private def validateQueryResponse(datasets: List[DataSet]):QueryCmdResult[DataSet] = {
    val name = cmdOptions.option[String]("name")
    if(datasets.length < 1){
      val msg = "No dataset with Name: %s could be found in %s".format(name, cmdScope.scope)
      throw new InternalErrorException(msg);
    }else if(datasets.length > 1){
      val msg = "Multiple data sets where found with Name: %s in %s".format(name, cmdScope.scope)
      throw new InternalErrorException(msg);
    }
    return QueryCmdResult[DataSet](datasets)
  }
}
