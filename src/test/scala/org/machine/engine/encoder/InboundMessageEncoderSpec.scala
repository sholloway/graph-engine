package org.machine.engine.encoder

import org.scalatest._
import org.scalatest.mock._

//Generated by sbt-protobuf
import org.machine.engine.messages.InboundMessageEnvelope.RequestMessage
import org.machine.engine.messages.InboundMessageEnvelope.RequestMessage.ActionType
import org.machine.engine.messages.InboundMessageEnvelope.RequestMessage.ScopeType
import org.machine.engine.messages.InboundMessageEnvelope.RequestMessage.EntityType
import org.machine.engine.messages.InboundMessageEnvelope.RequestMessage.PropertyDefinition
import org.machine.engine.messages.InboundMessageEnvelope.RequestMessage.Field

//https://developers.google.com/protocol-buffers/docs/javatutorial
//https://github.com/google/protobuf/blob/master/examples/AddPerson.java
class InboundMessageEncoderSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfter{
  describe("Inbound Message Encoding and Decoding"){
    it("should encode and decode an ElementDefinition message"){
      val messageBuilder = RequestMessage.newBuilder()

      messageBuilder
        .setAction(ActionType.UPDATE)
        .setScope(ScopeType.SCOPE_USER_SPACE)
        .setEntityType(EntityType.ELEMENT_DEFINITION)

      messageBuilder.getUserBuilder().setId("abc")

      messageBuilder.getElementDefinitionBuilder().setId("edId")
      messageBuilder.getElementDefinitionBuilder().setName("edName")
      messageBuilder.getElementDefinitionBuilder().setDescription("edDescription")

      //add PropertyDefinitions
      val pd1 = PropertyDefinition.newBuilder()
        .setId("pdId1")
        .setName("pd1")
        .setPropertyDefinitionType("String")
        .setDescription("a pd")
        .build()

      val pd2 = PropertyDefinition.newBuilder()
        .setId("pdId2")
        .setName("pd2")
        .setPropertyDefinitionType("String")
        .setDescription("a pd")
        .build()

      messageBuilder.getElementDefinitionBuilder().addProperties(pd1)
      messageBuilder.getElementDefinitionBuilder().addProperties(pd2)

      val bytes = messageBuilder.build().toByteArray()

      val decodedMsg = RequestMessage.newBuilder()
        .mergeFrom(bytes)

      decodedMsg.getAction should equal(ActionType.UPDATE)
      decodedMsg.getScope should equal(ScopeType.SCOPE_USER_SPACE)
      decodedMsg.getEntityType should equal(EntityType.ELEMENT_DEFINITION)
      decodedMsg.getUser().getId should equal("abc")

      decodedMsg.hasElementDefinition() should equal(true)
      decodedMsg.hasElement() should equal(false)
      decodedMsg.getElementDefinition().getId should equal("edId")
      decodedMsg.getElementDefinition().getName should equal("edName")
      decodedMsg.getElementDefinition().getDescription should equal("edDescription")
      decodedMsg.getElementDefinition().getPropertiesList() should have length 2
    }

    it("should encode and decode an Element message"){
      val messageBuilder = RequestMessage.newBuilder()
      messageBuilder
        .setAction(ActionType.CREATE)
        .setScope(ScopeType.SCOPE_DATA_SET)
        .setEntityType(EntityType.ELEMENT)

      messageBuilder.getUserBuilder().setId("Stevie")
      messageBuilder.getElementBuilder().setId("e1")
        .setElementType("edName")
        .setDescription("an element")

      val f1 = Field.newBuilder()
        .setName("f1")
        .setFieldType("int")
        .setValue("123456")
        .build()

      messageBuilder.getElementBuilder().addFields(f1)
      val bytes = messageBuilder.build().toByteArray()

      val decodedMsg = RequestMessage.newBuilder()
        .mergeFrom(bytes)

      decodedMsg.getAction should equal(ActionType.CREATE)
      decodedMsg.getScope should equal(ScopeType.SCOPE_DATA_SET)
      decodedMsg.getEntityType should equal(EntityType.ELEMENT)
      decodedMsg.getUser().getId should equal("Stevie")

      decodedMsg.hasElement() should equal(true)
      decodedMsg.hasElementDefinition() should equal(false)
      decodedMsg.getElement().getId should equal("e1")
      decodedMsg.getElement().getElementType should equal("edName")
      decodedMsg.getElement().getDescription should equal("an element")

      decodedMsg.getElement().getFieldsList() should have length 1
    }

    it("should encode associations")(pending)
  }
}
