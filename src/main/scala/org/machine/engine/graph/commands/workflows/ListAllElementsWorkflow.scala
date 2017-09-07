package org.machine.engine.graph.commands.workflows

import com.typesafe.scalalogging.{LazyLogging}
import org.neo4j.graphdb.GraphDatabaseService

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.machine.engine.exceptions.InternalErrorException
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.{CommandScope, CommandScopes}
import org.machine.engine.graph.nodes.Element

object ListAllElementsWorkflow extends LazyLogging{
  import Neo4JHelper._

  def workflow(capsule: CapsuleWithContext):CapsuleWithContext = {
    val wf = Function.chain(Seq(
      verifyRequiredCmdOptions,
      generateQuery,
      findElements
    ))
    return wf(capsule)
  }

  val FindElementsQuery                = "findElementsQuery"
  val ElementLabel                     = "element"
  val FoundElements                    = "Elements"
  val ElementsCouldNotBeListedErrorMsg = "Elements could not be created."

  val verifyRequiredCmdOptions = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = capsule._5 == Left(WorkflowStatuses.OK)
    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      val errorMsg:Option[String] = if (!capsule._3.contains(DataSetId) && !capsule._3.contains(DataSetName)){
        Some(DataSetFilterRequiredErrorMsg)
      }else{
        None
      }
      val status:Status = if(errorMsg.isEmpty) Left(WorkflowStatuses.OK) else Right(errorMsg.get)
      return (capsule._1, capsule._2, capsule._3, capsule._4, status)
    }
  }

  val generateQuery = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = capsule._5 == Left(WorkflowStatuses.OK) && capsule._2 == CommandScopes.DataSetScope
    def apply(capsule:CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      var status:Status = null
      try{
        val scopeFilter = generateScopeFilter(capsule._2, capsule._3)
        val query:String = """
        |match (ss:data_set) filter
        |match (ss)-[:contains]->(e:element)
        |return e as node, labels(e) as labels, keys(e) as keys
        """.stripMargin
        .replaceAll("filter", scopeFilter)
        capsule._4 += (FindElementsQuery -> query)
        status = Left(WorkflowStatuses.OK)
      }catch{
        case e:InternalErrorException => status = Right(e.getMessage())
      }
      return (capsule._1, capsule._2, capsule._3, capsule._4, status)
    }
  }

  val findElements = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = {
      capsule._5 == Left(WorkflowStatuses.OK) &&
      capsule._4.contains(FindElementsQuery)
    }

    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      var foundElements:Option[List[Element]] = None
      var status:Status = null
      try{
        transaction(capsule._1, (graphDB: GraphDatabaseService) => {
          val stmt = capsule._4(FindElementsQuery).toString()
          val elements = run[Element](graphDB,
            stmt,
            capsule._3.toJavaMap,
            elementProcessor)
          if(!elements.isEmpty){
            foundElements = Some(elements.toList)
          }
        })
        status = determineStatus(foundElements, capsule)
      }catch{
        case e: Throwable => {
          logger.error("Could not list elements.", e)
          status = Right(ElementDefinitionCreationFailureErrorMsg)
        }
      }
      return (capsule._1, capsule._2, capsule._3, capsule._4, status)
    }

    private def elementProcessor(results: ArrayBuffer[Element], record: java.util.Map[java.lang.String, Object]):ArrayBuffer[Element] = {
      val node                = record.get("node").asInstanceOf[org.neo4j.graphdb.Node]
      val keys:List[String]   = record.get("keys").asInstanceOf[java.util.List[String]].toList
      val labels:List[String] = record.get("labels").asInstanceOf[java.util.List[String]].toList

      val fields  = Map.empty[String, Any];
      val exclude = Seq(Mid, ElementDescriptionField, CreationTimeField, LastModifiedTimeField)

      keys.foreach(key => {
        if (!exclude.contains(key)){
          //Avoid possible exceptions with empty String default.
          val unknown = node.getProperty(key, Empty).asInstanceOf[Any]
          val assigned = unknown match {
            case x: Boolean => unknown.asInstanceOf[Boolean]
            case x: Byte => unknown.asInstanceOf[Byte]
            case x: Short => unknown.asInstanceOf[Short]
            case x: Int => unknown.asInstanceOf[Int]
            case x: Long => unknown.asInstanceOf[Long]
            case x: Float => unknown.asInstanceOf[Float]
            case x: Double => unknown.asInstanceOf[Double]
            case x: Char => unknown.asInstanceOf[Char]
            case x: String => unknown.asInstanceOf[String]
            case _ => {
              logger.debug(unknown.toString)
              Empty //I don't like this strategy...
            }
          }
          fields += (key -> assigned)
        }
      })

      /*
      Theoretically, there should always be two labels on an Element.
      1. The "element" label that indicates it is an Element.
      2. A label indicating what ElementDefinition was used to provision the Element.

      Multiple inheritence is not currently support for Elements.
      */
      val elementType =  labels.filterNot(label => label == ElementLabel).head

      val id                 = node.getProperty(Mid, Empty).toString();
      val elementDescription =  node.getProperty(ElementDescriptionField, Empty).toString();
      val creationType       =  node.getProperty(CreationTimeField, Empty).toString();
      val lastModifiedTime   =  node.getProperty(LastModifiedTimeField, Empty).toString();

      results += Element(id, elementType, elementDescription, fields.toMap,
        creationType, lastModifiedTime)
    }

    private def determineStatus(foundElements: Option[List[Element]],
      capsule: CapsuleWithContext):Status =
      if(foundElements.isDefined){ //Could legitimatlly have a length of 0.
        capsule._4 += (FoundElements -> foundElements.get)
        Left(WorkflowStatuses.OK)
      }else{
        Right(ElementsCouldNotBeListedErrorMsg)
      }
  }
}
