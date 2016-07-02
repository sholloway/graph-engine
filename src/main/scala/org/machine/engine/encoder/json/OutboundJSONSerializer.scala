package org.machine.engine.encoder.json

import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._

import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult, QueryCmdResult, UpdateCmdResult, DeleteCmdResult, InsertCmdResult}

object OutboundJSONSerializer{
  def serialize(result: EngineCmdResult):String = {
    result match {
      case query:  QueryCmdResult[_]  => findSerializer(query.results)
      case update: UpdateCmdResult[_] => serializeId(update.result)
      case delete: DeleteCmdResult[_] => serializeId(delete.result)
      case insert: InsertCmdResult[_] => serializeId(insert.result)
      case _ => {
        return throw new InternalErrorException(s"Could not find a matching JSON serializer for type $result")
      }
    }
  }

  private def serializeId(result: Any) = {
    s"""
    |{
    |  "id": "${result.toString}"
    |}
    """.stripMargin
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

  private def findSerializer(result: Seq[_]):String = {
    if (result.isEmpty){
      return ""
    }
    val resultType = findType(result.head)
    val response:String = if(typeOf[DataSet] =:= resultType){
      DataSetJSONSerializer.serialize(result.asInstanceOf[Seq[DataSet]])
    }else if(typeOf[ElementDefinition] =:= resultType){
      ElementDefinitionJSONSerializer.serialize(result.asInstanceOf[Seq[ElementDefinition]])
    }else if(typeOf[Element] =:= resultType){
      ElementJSONSerializer.serialize(result.asInstanceOf[Seq[Element]])
    }else if(typeOf[Association] =:= resultType){
      AssociationJSONSerializer.serialize(result.asInstanceOf[Seq[Association]])
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
