package org.machine.engine.graph.utilities

import org.machine.engine.graph.commands._
import org.neo4j.graphdb._
import scala.reflect.runtime.{universe => ru}

object DynamicCmdLoader{
  private val cmdPackageName = "org.machine.engine.graph.commands"

  /*
  Loads a command via reflection.
  String -> ClassSymbol -> Type -> Constructor
  */
  def provision(cmdName: String,
    database: GraphDatabaseService,
    cmdScope: CommandScope,
    commandOptions: GraphCommandOptions):InternalEngineCommand = {
    val className = s"$cmdPackageName.$cmdName"
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val classSymbol = mirror.staticClass(className)
    val cm = mirror.reflectClass(classSymbol)
    val classType = classSymbol.typeSignature //reflect.runtime.universe.Type
    val ctor = classType.decl(ru.termNames.CONSTRUCTOR).asMethod
    val cmdConstructor = cm.reflectConstructor(ctor)
    val cmd = cmdConstructor(database, cmdScope, commandOptions)
    return cmd.asInstanceOf[InternalEngineCommand]
  }
}
