package org.machine.engine.flow

import org.scalatest._
import scala.util.{Either, Left, Right}

import org.machine.engine.flow.requests.RequestRuleValidator

class RequestRuleValidatorSpec extends FunSpec with Matchers with BeforeAndAfterAll{
  import RequestRuleValidator._

  val validRequest = Map("user"->"Bob",
    "actionType"->"Create",
    "scope"->"UserSpace",
    "entityType"->"ElementDefinition",
    "filter"->"All")

  val missingUser =  Map("actionType"->"Retrieve",
    "scope"->"SystemSpace",
    "entityType"->"DataSet",
    "filter"->"Name")

  val missingActionType =  Map("user"->"Bob",
    "scope"->"SystemSpace",
    "entityType"->"Element",
    "filter"->"ID")

  val missingScope = Map("user"->"Bob",
    "actionType"->"Update",
    "entityType"->"Association",
    "filter"->"ID")

  val missingEntityType = Map("user"->"Bob",
    "actionType"->"Update",
    "scope"->"DataSet",
    "filter"->"ID")

  val missingFilter = Map("user"->"Bob",
    "actionType"->"Delete",
    "scope"->"DataSet",
    "entityType"->"Association")

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
