package org.machine.engine.graph.commands

import reflect.runtime.universe._
// import scala.collection._
// import scala.collection.generic._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}
import org.machine.engine.exceptions._

import org.machine.engine.graph.nodes.PropertyDefinitions

//http://daily-scala.blogspot.com/2010/04/creating-custom-traversable.html
class GraphCommandOptions{
  private var graphValue = Map.empty[String, AnyVal]
  private var graphObjects = Map.empty[String, AnyRef]

  def fieldValues = this.graphValue
  def fieldObjects = this.graphObjects
  def optionValues = this.graphValue
  def optionObjects = this.graphObjects

  def addField(fieldName: String, fieldValue: Any) = {
    fieldValue match {
      case x: Boolean => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Boolean])
      case x: Byte => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Byte])
      case x: Short => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Short])
      case x: Int => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Int])
      case x: Long => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Long])
      case x: Float => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Float])
      case x: Double => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Double])
      case x: Char => graphValue.+=(fieldName -> fieldValue.asInstanceOf[Char])
      case x: String => graphObjects.+=(fieldName -> fieldValue.asInstanceOf[String])
      case x: PropertyDefinitions => graphObjects.+=(fieldName -> fieldValue.asInstanceOf[PropertyDefinitions])
    }
  }

  def addOption(optionName:String, optionValue: Any) = {
    addField(optionName, optionValue)
  }

  def field[T: TypeTag](name: String):T = option[T](name)

  def option[T: TypeTag](name: String):T = {
    val map = name match {
      case v if typeOf[T] <:< typeOf[AnyVal] => graphValue
      case r if typeOf[T] <:< typeOf[AnyRef] => graphObjects
      case r if typeOf[T] <:< typeOf[Boolean] => graphValue
      case x if typeOf[T] <:< typeOf[Byte] => graphValue
      case x if typeOf[T] <:< typeOf[Short] => graphValue
      case x if typeOf[T] <:< typeOf[Int] => graphValue
      case x if typeOf[T] <:< typeOf[Long] => graphValue
      case x if typeOf[T] <:< typeOf[Float] => graphValue
      case x if typeOf[T] <:< typeOf[Double] => graphValue
      case x if typeOf[T] <:< typeOf[Char] => graphValue
      case _ => throw new InternalErrorException("GraphCommandOptions.option: Unsupported type.")
    }
    return map.get(name).getOrElse(throw new InternalErrorException("Could not find: %s".format(name))).asInstanceOf[T]
  }

  def contains(name: String):Boolean = {
    return graphValue.contains(name) || graphObjects.contains(name)
  }

  def reset:Unit = {
    graphValue = Map.empty[String, AnyVal]
    graphObjects = Map.empty[String, AnyRef]
  }

  //Create a Java Map
  def toJavaMap:java.util.HashMap[java.lang.String, Object] = {
    val map = new java.util.HashMap[java.lang.String, Object]()
    loadValues(map)
    graphObjects.foreach(t => map.put(t._1, t._2))
    return map
  }

  private def loadValues(javaMap: java.util.HashMap[java.lang.String, Object]):Unit = {
    graphValue.foreach(option => {
      val fieldName = option._1
      val fieldValue = option._2
      fieldValue match {
        case x: Boolean => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Boolean])
        case x: Byte => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Byte])
        case x: Short => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Short])
        case x: Int => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Integer])
        case x: Long => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Long])
        case x: Float => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Float])
        case x: Double => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Double])
        case x: Char => javaMap.put(fieldName, fieldValue.asInstanceOf[java.lang.Character])
        case _ => throw new InternalErrorException("GraphCommandOptions: Unhandled type.")
      }
    })
  }

  def toMap:scala.collection.immutable.Map[String, Any] = {
    val map = Map.empty[String, Any]
    graphValue.foreach(t => map.put(t._1, t._2))
    graphObjects.foreach(t => map.put(t._1, t._2))
    return map.toMap
  }

  def foreach(f: ((String, Any)) => Unit): Unit = {
    graphValue.foreach(f)
    graphObjects.foreach(f)
  }

  def keys: List[String] = {
    val items = ArrayBuffer.empty[String]
    graphValue.keys.foreach(key => items += key)
    graphObjects.keys.foreach(key => items += key)
    return items.toList
  }
}
