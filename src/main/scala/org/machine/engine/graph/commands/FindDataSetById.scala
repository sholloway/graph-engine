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

class FindDataSetById(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JQueryCommand[DataSet]{
  import Neo4JHelper._

  def execute():List[DataSet] = {
    logger.debug("FindDataSetById: Executing Command")
    val findDataSets = """
      |match (us:internal_user_space)-[:contains]->(ds:internal_data_set {mid:{dsId}})
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

  private def validateQueryResponse(datasets: List[DataSet]):List[DataSet] = {
    val mid = cmdOptions.option[String]("dsId")
    if(datasets.length < 1){
      val msg = "No dataset with mid: %s could be found in %s".format(mid, cmdScope.scope)
      throw new InternalErrorException(msg);
    }else if(datasets.length > 1){
      val msg = "Multiple data sets where found with mid: %s in %s".format(mid, cmdScope.scope)
      throw new InternalErrorException(msg);
    }
    return datasets
  }
}
