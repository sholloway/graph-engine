package org.machine.engine.authentication

import org.scalatest._
import org.scalatest.mock._

class PasswordSpec extends FunSpecLike with Matchers{
  import PasswordTools._;
  describe("Authentication with Passwords"){
    it ("should generate the same hash is pwd, iteration count and salt are identical"){
      val SALT_BYTE_SIZE = 64 // 512 bits
      val PBKDF2_ITERATIONS = 20000
      val password="hello world"
      val salt = generateSalt(SALT_BYTE_SIZE)
      val hashA = generateHash(password, salt, PBKDF2_ITERATIONS)
      val hashB = generateHash(password, salt, PBKDF2_ITERATIONS)
      val hashEquality:Boolean = compare(hashA, hashB)
      hashEquality should be(true)
    }

    it ("should be able to encode and decode to base64"){
      val SALT_BYTE_SIZE = 64 // 512 bits
      val PBKDF2_ITERATIONS = 20000
      val password="hello world"
      val salt = generateSalt(SALT_BYTE_SIZE)
      val originalHash = generateHash(password, salt, PBKDF2_ITERATIONS)
      val encodedHash:String = hashToBase64(originalHash)
      val decodedHash:Array[Byte] = base64ToHash(encodedHash)
      val hashEquality:Boolean = compare(originalHash, decodedHash)
      hashEquality should be(true)
    }
  }
}
