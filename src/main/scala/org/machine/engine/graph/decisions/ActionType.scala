package org.machine.engine.graph.decisions

sealed trait ActionType{
  def value:String
}

object ActionTypes{
  case object Create extends ActionType{val value="create";}
  case object Retrieve extends ActionType{val value="retrieve";}
  case object Update extends ActionType{val value="update";}
  case object Delete extends ActionType{val value="delete";}
  case object None extends ActionType{val value="none";}
}
