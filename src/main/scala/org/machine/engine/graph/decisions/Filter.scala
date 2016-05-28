package org.machine.engine.graph.decisions

sealed trait Filter{
  def value:String
}

object Filters{
  case object ID extends Filter{val value="ID";}
  case object Name extends Filter{val value="Name";}
  case object All extends Filter{val value="All";}
  case object None extends Filter{val value="None";}

  def pickFilter(alias: String):Filter = {
    alias match {
      case "ID" => ID
      case "Name" => Name
      case "All" => All
      case _ => None
    }
  }
}
