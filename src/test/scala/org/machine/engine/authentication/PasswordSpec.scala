package org.machine.engine.authentication

import java.util.Random;
import org.scalatest._
import org.scalatest.mock._

class PasswordSpec extends FunSpecLike with Matchers with BeforeAndAfterAll{
  import PasswordTools._;
  var generator:Random = null

  override def beforeAll(){
    val seed = generateSeed()
    generator = createRandomNumberGenerator(seed)
  }

  describe("Authentication with Passwords"){
    it ("should generate the same hash is pwd, iteration count and salt are identical"){
      val SALT_BYTE_SIZE = 64 // 512 bits
      val PBKDF2_ITERATIONS = 20000
      val password="hello world"
      val salt = generateSalt(generator, SALT_BYTE_SIZE)
      val hashA = generateHash(password, salt, PBKDF2_ITERATIONS)
      val hashB = generateHash(password, salt, PBKDF2_ITERATIONS)
      val hashEquality:Boolean = compare(hashA, hashB)
      hashEquality should be(true)
    }

    it ("should be able to encode and decode to base64"){
      val SALT_BYTE_SIZE = 64 // 512 bits
      val PBKDF2_ITERATIONS = 20000
      val password="hello world"
      val salt = generateSalt(generator,SALT_BYTE_SIZE)
      val originalHash = generateHash(password, salt, PBKDF2_ITERATIONS)
      val encodedHash:String = hashToBase64(originalHash)
      val decodedHash:Array[Byte] = base64ToHash(encodedHash)
      val hashEquality:Boolean = compare(originalHash, decodedHash)
      hashEquality should be(true)
    }
  }

  it ("should generate a unique, fixed size session id"){
    val SESSION_BYTE_SIZE = 128
    val sessionIdBytesA = generateSessionId(generator, SESSION_BYTE_SIZE)
    val encodedSessionIdA = byteArrayToHexStr(sessionIdBytesA)
    val sessionIdBytesB = generateSessionId(generator, SESSION_BYTE_SIZE)
    val encodedSessionIdB = byteArrayToHexStr(sessionIdBytesB)
    encodedSessionIdA.length should be(40)
    encodedSessionIdB.length should be(40)
    encodedSessionIdA should not equal(encodedSessionIdB)
  }
}
