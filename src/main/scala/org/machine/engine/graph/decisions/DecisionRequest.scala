package org.machine.engine.graph.decisions

import org.machine.engine.graph.commands.CommandScope

case class DecisionRequest(user: Option[String],
  actionType: ActionType,
  scope: CommandScope,
  entityType: EntityType,
  filter: Filter
){
  def toMap():Map[String, Option[String]] = {
    return Map("user" -> user,
      "actionType" -> Some(actionType.value),
      "scope" -> Some(scope.scope),
      "entityType" -> Some(entityType.value),
      "filter" -> Some(filter.value))
  }
}
