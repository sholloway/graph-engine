package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class FindOutboundAssociationsByElementId(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4JQueryCommand[Association] with LazyLogging{
  import Neo4JHelper._

  def execute():QueryCmdResult[Association] = {
    logger.debug("FindOutboundAssociationsByElementId: Executing Command")
    return QueryCmdResult(findAssociations(database, cmdOptions))
  }

  private def findAssociations(database: GraphDatabaseService, cmdOptions: GraphCommandOptions):List[Association] = {
    val statement = """
    |match (start)-[association]->(end)
    |where start.mid={elementId}
    |return start.mid as startingElementId,
    |   end.mid as endingElementId,
    |   type(association) as associationType,
    |   keys(association) as keys,
    |   association.association_time as association_time,
    |   association.last_modified_time as last_modified_time,
    |   association.associationId as associationId,
    |   association as data
    """.stripMargin
    val records = query[Association](database,
      statement,
      cmdOptions.toJavaMap,
      associationMapper)
    return records.toList.sortWith(compare)
  }

  private def compare(a1: Association, a2: Association):Boolean = {
    return a1.associationType.compareToIgnoreCase(a2.associationType) < 0
  }

  private def associationMapper(results: ArrayBuffer[Association],
    record: java.util.Map[java.lang.String, Object]) = {
      val keys:List[String] = record.get("keys").asInstanceOf[java.util.List[String]].toList
      val neo4JRelationshipProxy = record.get("data").asInstanceOf[org.neo4j.kernel.impl.core.RelationshipProxy]

      val associationId:String = mapString("associationId", record, true)
      val associationType = mapString("associationType", record, true)
      val associationTime:String = mapString("association_time", record, true)
      val lastModifiedTime:String = mapString("last_modified_time", record, false)
      val startingElementId = mapString("startingElementId", record, true)
      val endingElementId = mapString("endingElementId", record, true)

      val fields: Map[String, Any] = Map.empty[String, Any]
      val exclude = List("associationId", "association_time", "last_modified_time")

      keys.foreach(key => {
        if (!exclude.contains(key)){
          val value: Any = neo4JRelationshipProxy.getProperty(key)
          val temp = value match {
            case x: Boolean => value.asInstanceOf[Boolean]
            case x: Byte => value.asInstanceOf[Byte]
            case x: Short => value.asInstanceOf[Short]
            case x: Int => value.asInstanceOf[Int]
            case x: Long => value.asInstanceOf[Long]
            case x: Float => value.asInstanceOf[Float]
            case x: Double => value.asInstanceOf[Double]
            case x: Char => value.asInstanceOf[Char]
            case x: String => value.asInstanceOf[String]
            case _ => logger.debug(value.toString)
          }
          fields += (key -> temp)
        }
      })

    results += new Association(associationId, associationType, fields.toMap,
      startingElementId,endingElementId,associationTime,lastModifiedTime)
  }
}
