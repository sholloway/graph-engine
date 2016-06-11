package org.machine.engine

import org.scalatest._
import scala.util.{Either, Left, Right}
import org.machine.engine.exceptions._

class PartialFunctionsSpike extends FunSpec with Matchers with BeforeAndAfterAll{
  import RuleValidator._

  val validRequest = Map("user"->"Bob",
    "actionType"->"blah",
    "scope"->"asfsf",
    "entityType"->"asfs",
    "filter"->"r32f")

  val missingUser =  Map("actionType"->"blah",
    "scope"->"asfsf",
    "entityType"->"asfs",
    "filter"->"r32f")

  val missingActionType =  Map("user"->"Bob",
    "scope"->"asfsf",
    "entityType"->"asfs",
    "filter"->"r32f")

  val missingScope = Map("user"->"Bob",
    "actionType"->"blah",
    "entityType"->"asfs",
    "filter"->"r32f")

  val missingEntityType = Map("user"->"Bob",
    "actionType"->"blah",
    "scope"->"asfsf",
    "filter"->"r32f")

  val missingFilter = Map("user"->"Bob",
    "actionType"->"blah",
    "scope"->"asfsf",
    "entityType"->"asfs")

  override def beforeAll(){}
  override def afterAll(){}

  /*
  Thoughts:
  Could possibly use Either[Map[String, Any], String] for response.
  Could have isDefinedAt fail if the string is present.

  Chaining will require the output be the same as the input.
  */
  describe("Partial Functions"){
    it ("should be map on a valid request"){
      validate(Left(validRequest)) should equal(Left(validRequest))
    }

    it ("should invalidate a request missing a user"){
      validate(Left(missingUser)) should equal(Right("The request must contain a valid user."))
    }

    it ("should invalidate a request missing a action type"){
      validate(Left(missingActionType)) should equal(Right("The request must contain a valid action type."))
    }

    it ("should invalidate a request missing a scope"){
      validate(Left(missingScope)) should equal(Right("The request must contain a valid scope."))
    }

    it ("should invalidate a request missing a entity type"){
      validate(Left(missingEntityType)) should equal(Right("The request must contain a valid entity type."))
    }

    it ("should invalidate a request missing a filter"){
      validate(Left(missingFilter)) should equal(Right("The request must contain a valid filter."))
    }
  }
}

object RuleValidator{
  type ValidationFailure = String
  type ValidationInput = Either[Map[String, Any], ValidationFailure]

  def validate(input: ValidationInput):ValidationInput = validateRule(input)

  private def genericIsDefined(input: ValidationInput):Boolean = input match {
    case Left(x) => true
    case Right(x) => false
    case _ => throw new InternalErrorException("Woops...")
  }

  private val validateUser = new PartialFunction[ValidationInput,ValidationInput]{
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        if(either.left.get.contains("user")){
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
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        if(either.left.get.contains("actionType")){
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
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        if(either.left.get.contains("scope")){
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
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        if(either.left.get.contains("entityType")){
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
    def isDefinedAt(input: ValidationInput):Boolean = genericIsDefined(input)
    def apply(either: ValidationInput):ValidationInput = {
      if(isDefinedAt(either)){
        if(either.left.get.contains("filter")){
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
