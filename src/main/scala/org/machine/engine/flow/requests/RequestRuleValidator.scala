package org.machine.engine.flow.requests

import scala.util.{Either, Left, Right}

import org.machine.engine.exceptions._
import org.machine.engine.graph.commands.CommandScopes
import org.machine.engine.graph.decisions.{ActionTypes, EntityTypes, Filters}

object RequestRuleValidator{
  type ValidationFailure = String
  type ValidationInput = Either[Map[String, Any], ValidationFailure]

  def validate(input: ValidationInput):ValidationInput = validateRule(input)

  private def genericIsDefined(input: ValidationInput):Boolean = input match {
    case Left(x) => true
    case Right(x) => false
    case _ => throw new InternalErrorException("Woops...")
  }

  private val validateUser = new PartialFunction[ValidationInput,ValidationInput]{
    private val parameter = "user"
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        if(either.left.get.contains(parameter)){
          return Left(either.left.get)
        }else{
          return Right("The request must contain a valid user.")
        }
      }else{
        return either
      }
    }
  }

  private val validateActionType = new PartialFunction[ValidationInput, ValidationInput]{
    private val parameter = "actionType"
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        val request = either.left.get
        if(request.contains(parameter) &&
          ActionTypes.validTypes.contains(request(parameter))){
          return Left(either.left.get)
        }else{
          return Right("The request must contain a valid action type.")
        }
      }else{
        return either
      }
    }
  }

  private val validateScope = new PartialFunction[ValidationInput, ValidationInput]{
    private val parameter = "scope"
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        val request = either.left.get
        if(request.contains(parameter) &&
          CommandScopes.validScopes.contains(request(parameter))){
          return Left(either.left.get)
        }else{
          return Right("The request must contain a valid scope.")
        }
      }else{
        return either
      }
    }
  }

  private val validateEntityType = new PartialFunction[ValidationInput, ValidationInput]{
    private val parameter = "entityType"
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        val request = either.left.get
        if(request.contains(parameter) &&
          EntityTypes.validTypes.contains(request(parameter))){
          return Left(either.left.get)
        }else{
          return Right("The request must contain a valid entity type.")
        }
      }else{
        return either
      }
    }
  }

  private val validateFilter = new PartialFunction[ValidationInput, ValidationInput]{
    private val parameter =  "filter"
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        val request = either.left.get
        if(request.contains(parameter) &&
          Filters.validFilters.contains(request(parameter))){
          return Left(either.left.get)
        }else{
          return Right("The request must contain a valid filter.")
        }
      }else{
        return either
      }
    }
  }

  private val validateRule = Function.chain(Seq(validateUser,
    validateActionType,
    validateScope,
    validateEntityType,
    validateFilter))
}
