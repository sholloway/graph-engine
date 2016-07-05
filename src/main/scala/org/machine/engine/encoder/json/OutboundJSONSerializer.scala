package org.machine.engine.encoder.json

import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._

import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult,
  EngineCmdResultStatuses, QueryCmdResult, UpdateCmdResult, DeleteCmdResult,
  InsertCmdResult, DeleteSetCmdResult}

object OutboundJSONSerializer{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._

  def serialize(result: EngineCmdResult):String = {
    if (result.status != EngineCmdResultStatuses.OK){
      return serializeStatus(result)
    }else{
      result match {
        case query:  QueryCmdResult[_]  => findSerializer(query, query.results)
        case update: UpdateCmdResult[_] => serializeResult(update, update.result)
        case delete: DeleteCmdResult[_] => serializeResult(delete, delete.result)
        case insert: InsertCmdResult[_] => serializeResult(insert, insert.result)
        case deleteSet: DeleteSetCmdResult => serializeStatus(deleteSet)
        case _ => {
          return throw new InternalErrorException(s"Could not find a matching JSON serializer for type $result")
        }
      }
    }
  }

  private def serializeResult(result: EngineCmdResult, resultValue: Any):String = {
    val json =
      (
        ("status" -> result.status.value) ~
        ("errorMessage" -> result.errorMessage) ~
        ("id" -> resultValue.toString)
      )
      return prettyRender(json)
  }

  private def serializeStatus(result: EngineCmdResult):String = {
    val json =
      (
        ("status" -> result.status.value) ~
        ("errorMessage" -> result.errorMessage)
      )
    return prettyRender(json)
  }

  /*
  Playing 20 questions because erasure is removing the TypeTag on the Sequence.
  */
  private def findType(obj: Any):Type = {
    if(obj.isInstanceOf[DataSet]){
      return typeOf[DataSet]
    }else if(obj.isInstanceOf[ElementDefinition]){
      return typeOf[ElementDefinition]
    }else if(obj.isInstanceOf[Element]){
      return typeOf[Element]
    }else if(obj.isInstanceOf[Association]){
      return typeOf[Association]
    }else if(obj.isInstanceOf[AssociationDefinition]){
      return typeOf[AssociationDefinition]
    }else if(obj.isInstanceOf[PropertyDefinition]){
      return typeOf[PropertyDefinition]
    }else{
      return typeOf[Any]
    }
  }

  private def findSerializer(result: EngineCmdResult, resultValues: Seq[_]):String = {
    if (resultValues.isEmpty){
      return serializeStatus(result)
    }
    val resultType = findType(resultValues.head)
    val response:String = if(typeOf[DataSet] =:= resultType){
      DataSetJSONSerializer.serialize(result, resultValues.asInstanceOf[Seq[DataSet]])
    }else if(typeOf[ElementDefinition] =:= resultType){
      ElementDefinitionJSONSerializer.serialize(result, resultValues.asInstanceOf[Seq[ElementDefinition]])
    }else if(typeOf[Element] =:= resultType){
      ElementJSONSerializer.serialize(result, resultValues.asInstanceOf[Seq[Element]])
    }else if(typeOf[Association] =:= resultType){
      AssociationJSONSerializer.serialize(result, resultValues.asInstanceOf[Seq[Association]])
    }else{
      throw new InternalErrorException(s"Could not find a matching JSON serializer for type $resultType")
    }
    return response
  }
}


// def paramInfo[T](x: T)(implicit tag: TypeTag[T]): Unit = {
//   val targs = tag.tpe match { case TypeRef(_, _, args) => args }
//   println(s"type of $x has type arguments $targs")
// }

// def info[T](x: T)(implicit tag: TypeTag[T]): Type = {
//   return tag.tpe
// }
