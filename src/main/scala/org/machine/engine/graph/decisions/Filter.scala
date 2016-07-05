package org.machine.engine.graph.decisions

sealed trait Filter{
  def value:String
}

object Filters{
  case object ID extends Filter{val value="ID";}
  case object Name extends Filter{val value="Name";}
  case object All extends Filter{val value="All";}
  case object None extends Filter{val value="None";}
  case object Downstream extends Filter{val value="Downstream";}
  case object Upstream extends Filter{val value="Upstream";}

  def pickFilter(alias: String):Filter = {
    alias match {
      case "ID" => ID
      case "Name" => Name
      case "All" => All
      case "Downstream" => Downstream
      case "Upstream" => Upstream
      case _ => None
    }
  }

  def validFilters = Seq("ID", "Name", "All", "None", "Downstream", "Upstream")
}
