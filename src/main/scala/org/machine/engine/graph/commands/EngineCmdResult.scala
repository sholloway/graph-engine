package org.machine.engine.graph.commands

sealed trait EngineCmdResultStatus{
  def value:String;
}

/*
Note:
Value is of type String here due to we might expand to an HTTP style
of codes
*/
object EngineCmdResultStatuses{
  case object OK extends EngineCmdResultStatus{val value = "ok"}
  case object Error extends EngineCmdResultStatus{val value = "error"}
}

trait EngineCmdResult{
  def status:EngineCmdResultStatus;
  def errorMessage:Option[String];
}

case class QueryCmdResult[T](val results: Seq[T],
  val status: EngineCmdResultStatus = EngineCmdResultStatuses.OK,
  val errorMessage:Option[String] = None) extends EngineCmdResult

case class UpdateCmdResult[T](val result: T,
  val status: EngineCmdResultStatus = EngineCmdResultStatuses.OK,
  val errorMessage:Option[String] = None) extends EngineCmdResult

case class DeleteCmdResult[T](val result: T,
  val status: EngineCmdResultStatus = EngineCmdResultStatuses.OK,
  val errorMessage:Option[String] = None) extends EngineCmdResult

case class InsertCmdResult[T](val result: T,
  val status: EngineCmdResultStatus = EngineCmdResultStatuses.OK,
  val errorMessage:Option[String] = None) extends EngineCmdResult
