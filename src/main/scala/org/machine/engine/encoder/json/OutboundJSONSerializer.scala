package org.machine.engine.encoder.json

import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._

import org.machine.engine.graph.nodes.DataSet
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult, QueryCmdResult, UpdateCmdResult, DeleteCmdResult, InsertCmdResult}

object OutboundJSONSerializer{
  def serialize(result: EngineCmdResult):String = {
    result match {
      case query: QueryCmdResult[_] => {
        matchQueryResultType(query.results)
      }
      case _ => {
        Console.println("failed to match for QueryCmdResult")
        result.toString
      }
    }
  }

  private def matchQueryResultType(result: Seq[_]):String = {
    val resultType = findType(result.head)
    resultType match {
      case _ : DataSet => DataSetJSONSerializer.serialize(result.asInstanceOf[Seq[DataSet]])
      case _ => {
        Console.println("Could not match Seq[DataSet]")
        result.toString
      }
    }
  }

  private def typeInfo[T](x: T)(implicit tag: TypeTag[T]): Type = {
    return tag.tpe
  }

  private def findType(obj: Any):Type = {
    if(obj.isInstanceOf[DataSet]){
      return typeOf[DataSet]
    }else{
      return typeOf[Any]
    }
  }
}

/*
def paramInfo[T](x: T)(implicit tag: TypeTag[T]): Unit = {
  val targs = tag.tpe match { case TypeRef(_, _, args) => args }
  println(s"type of $x has type arguments $targs")
}

def info[T](x: T)(implicit tag: TypeTag[T]): Type = {
  return tag.tpe
}
*/
