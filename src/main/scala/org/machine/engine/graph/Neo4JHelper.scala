package org.machine.engine.graph

import org.neo4j.graphdb._
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object Neo4JHelper{
  def transaction(graphDB:GraphDatabaseService, tx:GraphDatabaseService => Unit){
    var dbTransactionOption: Option[Transaction] = None
    try{
      dbTransactionOption = Some(graphDB.beginTx());
      tx(graphDB)
      dbTransactionOption.foreach(t => t.success())
    }catch{
      case e:Throwable => e.printStackTrace
    }finally{
      dbTransactionOption.foreach(tx => tx.close())
    }
  }

  /*
    Generic query function.
    http://www.brentsowers.com/2011/11/writing-generic-functions-that-take.html
    https://stackoverflow.com/questions/3213510/what-is-a-manifest-in-scala-and-when-do-you-need-it

    Use:
    val books = query[Book](db, cypher,
      (results:ArrayBuffer[Book],
        record: java.util.Map[java.lang.String, Object]) => {
      val id = record.get("id")
      val title = record.get("title")
      results += new Book(id.asInstanceOf[Long], title.toString())
    })
  */
  // def query[T:Manifest](graphDB:GraphDatabaseService,
  //   query:String,
  //   recordHandler:(ArrayBuffer[T],
  //     java.util.Map[java.lang.String, Object]) => Unit):Array[T] = {
  //   var dbTransactionOption: Option[Transaction] = None
  //   var resultOption: Option[Result] = None
  //   var results = new ArrayBuffer[T]()
  //   try{
  //     dbTransactionOption = Some(graphDB.beginTx());
  //     resultOption = Some(graphDB.execute(query))
  //     while(resultOption.get.hasNext()){
  //       val record = resultOption.get.next()
  //       recordHandler(results, record)
  //     }
  //   }catch{
  //     case e:Throwable => e.printStackTrace
  //   }finally{
  //     resultOption.foreach(_.close())
  //     dbTransactionOption.foreach(tx => tx.close())
  //   }
  //   return results.toArray
  // }

  def query[T:Manifest](graphDB: GraphDatabaseService,
    query: String,
    params: java.util.Map[java.lang.String, Object],
    recordHandler: (ArrayBuffer[T],
      java.util.Map[java.lang.String, Object]) => Unit):Array[T] = {
    val queryParamsOption: Option[java.util.Map[java.lang.String, Object]] = Option(params).orElse(None)
    var dbTransactionOption: Option[Transaction] = None
    var resultOption: Option[Result] = None
    var results = new ArrayBuffer[T]()
    try{
      dbTransactionOption = Some(graphDB.beginTx());
      if (queryParamsOption.isDefined){
        resultOption = Some(graphDB.execute(query, queryParamsOption.get))
      }else{        
        resultOption = Some(graphDB.execute(query))
      }
      while(resultOption.get.hasNext()){
        val record = resultOption.get.next()
        recordHandler(results, record)
      }
    }catch{
      case e:Throwable => e.printStackTrace
    }finally{
      resultOption.foreach(_.close())
      dbTransactionOption.foreach(tx => tx.close())
    }
    return results.toArray
  }
}
