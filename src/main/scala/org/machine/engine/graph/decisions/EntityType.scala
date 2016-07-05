package org.machine.engine.graph.decisions

sealed trait EntityType{
  def value:String
}

object EntityTypes{
  case object ElementDefinition extends EntityType{val value="ElementDefinition";}
  case object DataSet extends EntityType{val value="DataSet";}
  case object Element extends EntityType{val value="Element";}
  case object ElementField extends EntityType{val value="ElementField";}
  case object Association extends EntityType{val value="Association";}
  case object AssociationField extends EntityType{val value="AssociationField";}
  case object InboundAssociation extends EntityType{val value="InboundAssociation";}
  case object OutboundAssociation extends EntityType{val value="OutboundAssociation";}
  case object PropertyDefinition extends EntityType{val value="PropertyDefinition";}
  case object None extends EntityType{val value="None";}

  def pickEntity(alias: String):EntityType = {
    alias match {
      case "ElementDefinition"    => ElementDefinition
      case "DataSet"              => DataSet
      case "Element"              => Element
      case "ElementField"         => ElementField
      case "Association"          => Association
      case "AssociationField"     => AssociationField
      case "InboundAssociation"   => InboundAssociation
      case "OutboundAssociation"  => OutboundAssociation
      case "PropertyDefinition"   => PropertyDefinition
      case _                      => None
    }
  }

  def validTypes = Seq("ElementDefinition",
    "DataSet",
    "Element",
    "ElementField",
    "PropertyDefinition",
    "Association",
    "AssociationField",
    "InboundAssociation",
    "OutboundAssociation"
  )
}
