package org.machine.engine.graph.decisions

sealed trait ActionType{
  def value:String
}

object ActionTypes{
  case object Create extends ActionType{val value="Create";}
  case object Retrieve extends ActionType{val value="Retrieve";}
  case object Update extends ActionType{val value="Update";}
  case object Delete extends ActionType{val value="Delete";}
  case object None extends ActionType{val value="None";}

  def pickAction(alias: String):ActionType = {
    alias match {
      case "Create"   => Create
      case "Retrieve" => Retrieve
      case "Update"   => Update
      case "Delete"   => Delete
      case _          => None
    }
  }

  def validTypes = Seq("Create",
    "Retrieve",
    "Update",
    "Delete")
}
