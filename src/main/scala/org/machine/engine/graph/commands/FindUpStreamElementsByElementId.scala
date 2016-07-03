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

class FindUpStreamElementsByElementId(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4JQueryCommand[Element] with LazyLogging{
  import Neo4JHelper._
  def execute():QueryCmdResult[Element] = {
    logger.debug("FindUpStreamElementsByElementId: Executing Command")
    val elements = findElements(database, cmdOptions)
    return QueryCmdResult(filterElements(elements))
  }

  private def findElements(database: GraphDatabaseService, cmdOptions: GraphCommandOptions):List[Element] = {
    val statement = """
    |match (start)-[]->(end)
    |where end.mid={elementId}
    |return start.mid as elementId,
    |   labels(start) as elementTypes,
    |   keys(start) as keys,
    |   start.element_description as element_description,
    |   start.creation_time as creation_time,
    |   start.last_modified_time as last_modified_time,
    |   start as data
    """.stripMargin
    val records = query[Element](database,
      statement,
      cmdOptions.toJavaMap,
      elementMapper)
    return records.toList.sortWith(compare)
  }

  private def compare(e1: Element, e2: Element):Boolean = {
    return e1.elementType.compareToIgnoreCase(e2.elementType) < 0
  }

  private def elementMapper(results: ArrayBuffer[Element],
    record: java.util.Map[java.lang.String, Object]) = {
      val keys:List[String] = record.get("keys").asInstanceOf[java.util.List[String]].toList
      val neo4JNodeProxy    = record.get("data").asInstanceOf[org.neo4j.kernel.impl.core.NodeProxy]

      val elementId:String          = mapString("elementId", record, true)
      val elementTypes:List[String] = record.get("elementTypes").asInstanceOf[java.util.List[String]].toList
      val elementDescription        = mapString("element_description", record, true)
      val creationTime:String       = mapString("creation_time", record, true)
      val lastModifiedTime:String   = mapString("last_modified_time", record, false)

      val fields: Map[String, Any] = Map.empty[String, Any]
      val exclude     = List("elementId", "creation_time", "creation_time")
      val elementType = elementTypes.filterNot(l => l == "element").head

      keys.foreach(key => {
        if (!exclude.contains(key)){
          val value: Any = neo4JNodeProxy.getProperty(key)
          val temp = value match {
            case x: Boolean => value.asInstanceOf[Boolean]
            case x: Byte    => value.asInstanceOf[Byte]
            case x: Short   => value.asInstanceOf[Short]
            case x: Int     => value.asInstanceOf[Int]
            case x: Long    => value.asInstanceOf[Long]
            case x: Float   => value.asInstanceOf[Float]
            case x: Double  => value.asInstanceOf[Double]
            case x: Char    => value.asInstanceOf[Char]
            case x: String  => value.asInstanceOf[String]
            case _          => logger.debug(value.toString)
          }
          fields += (key -> temp)
        }
      })

    results += new Element(elementId, elementType, elementDescription, fields.toMap,
      creationTime, lastModifiedTime)
  }

  /** Remove the (dataset)-[:contains]->(element) relationship.
  */
  private def filterElements(elements:List[Element]):List[Element] = {
    return elements.filterNot(e => e.elementType == "internal_data_set")
  }
}
