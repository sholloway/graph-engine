package org.machine.engine.graph.decisions

sealed trait EntityType{
  def value:String
}

object EntityTypes{
  case object ElementDefinition extends EntityType{val value="ElementDefinition";}
  case object DataSet extends EntityType{val value="DataSet";}
  case object Element extends EntityType{val value="Element";}
  case object Association extends EntityType{val value="Association";}
  case object None extends EntityType{val value="None";}
}
