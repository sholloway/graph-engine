package org.machine.engine.graph.commands

trait EngineCmdResult{}
case class QueryCmdResult[T](val results: Seq[T]) extends EngineCmdResult
case class UpdateCmdResult[T](val result: T) extends EngineCmdResult{}
case class DeleteCmdResult[T](val result: T) extends EngineCmdResult{}
case class InsertCmdResult[T](val result: T) extends EngineCmdResult{}
