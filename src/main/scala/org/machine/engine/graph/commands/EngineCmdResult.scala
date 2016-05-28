package org.machine.engine.graph.commands

trait EngineCmdResult{
}

case class QueryCmdResult[T](val results: Seq[T]) extends EngineCmdResult

class UpdateCmdResult extends EngineCmdResult{}
class DeleteCmdResult extends EngineCmdResult{}
class InsertCmdResult extends EngineCmdResult{}
