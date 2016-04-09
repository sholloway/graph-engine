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

class FindAssociationById(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends LazyLogging{
  import Neo4JHelper._

  def execute():Association = {
    logger.debug("FindAssociationById: Executing Command")
    val associationDefinition:AssociationDefinition = findAssociationStructure(database, cmdOptions)
    return findAssociation(database, cmdOptions, associationDefinition)
  }

  private def findAssociationStructure(database: GraphDatabaseService,
    cmdOptions: GraphCommandOptions):AssociationDefinition = {
    val statement = """
    match (x)-[association]->(y)
    where association.associationId = {associationId}
    return type(association) as associationType, keys(association) as keys
    """.stripMargin
    val records = query[AssociationDefinition](database,
      statement,
      cmdOptions.toJavaMap,
      associationStructureMapper)
    validateAssociationStructureResults(records.toList, cmdOptions)
    return records.head
  }

  private def validateAssociationStructureResults(records:List[AssociationDefinition],
    cmdOptions: GraphCommandOptions) = {
    val associationId = cmdOptions.option[String]("associationId")
    if (records.length < 1) {
      val msg = "No association with associationId: %s could be found.".format(associationId)
      throw new InternalErrorException(msg)
    }else if(records.length > 1){
      val msg = "Multiple associations where found with associationId: %s".format(associationId)
      throw new InternalErrorException(msg)
    }
  }

  private def associationStructureMapper(results: ArrayBuffer[AssociationDefinition],
    record: java.util.Map[java.lang.String, Object]) = {
      val label = record.get("associationType").asInstanceOf[String]
      val keys:List[String] = record.get("keys").asInstanceOf[java.util.List[String]].toList
      results += new AssociationDefinition(label, keys)
  }

  private def findAssociation(database: GraphDatabaseService,
    cmdOptions: GraphCommandOptions,
    associationDefinition: AssociationDefinition
  ):Association = {
    val statement = buildFindAssociationQuery(cmdOptions, associationDefinition);
    val associations = query[Association](database, statement, cmdOptions.toJavaMap, associationMapper)
    return validateAssociation(associations.toList, cmdOptions)
  }

  private def buildFindAssociationQuery(cmdOptions: GraphCommandOptions,
    associationDefinition: AssociationDefinition
  ):String = {
    val prefix = "association"
    val exclude:List[String] = List()
    val fetchClause = buildFetchClause(prefix, associationDefinition.keys, exclude)
    val statement = """
    match (x)-[association {associationId:{associationId}}]->(y)
    return x.mid as startingElementId, y.mid as endingElementId, type(association) as associationType, fetchClause
    """.stripMargin
      .replaceAll("fetchClause", fetchClause)
    return statement
  }

  private def associationMapper(
    results: ArrayBuffer[Association],
    record: java.util.Map[java.lang.String, Object]):Unit = {
    val startingElementId = mapString("startingElementId", record, true)
    val endingElementId = mapString("endingElementId", record, true)
    val associationType = mapString("associationType", record, true)
    val associationId:String = mapString("associationId", record, true)
    val associationTime:String = mapString("association_time", record, true)
    val lastModifiedTime:String = mapString("last_modified_time", record, false)
    val fields: Map[String, Any] = Map.empty[String, Any]
    val exclude = List("startingElementId", "endingElementId", "associationType",
      "associationId", "dsId", "association_time", "last_modified_time")
    record.keys.foreach(key => {
      if(!exclude.contains(key)){
        val value: Any = record.get(key)
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
      startingElementId, endingElementId, associationTime, lastModifiedTime)
  }

  private def cast[T:Manifest](x: T):T = x.asInstanceOf[T]

  private def validateAssociation(associations: List[Association], cmdOptions: GraphCommandOptions):Association = {
    val associationId = cmdOptions.option[String]("associationId")
    if(associations.length < 1){
      val msg = "No association with associationId: %s could be found.".format(associationId)
      throw new InternalErrorException(msg);
    }else if(associations.length > 1){
      val msg = "Multiple associations were found with associationId: %s".format(associationId)
      throw new InternalErrorException(msg);
    }
    return associations.head
  }
}
