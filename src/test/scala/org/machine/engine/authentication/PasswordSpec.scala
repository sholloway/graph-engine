package org.machine.engine.authentication

import org.scalatest._
import org.scalatest.mock._

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordSpec extends FunSpecLike with Matchers{
  describe("Authentication with Passwords"){
    it ("should generate the same hash is pwd, iteration count and salt are identical"){
      val HASH_BYTE_SIZE = 64 // 512 bits
      val SALT_BYTE_SIZE = 64 // 512 bits
      val PBKDF2_ITERATIONS = 20000
      val password="hello world"
      val salt = generateSeed(SALT_BYTE_SIZE)
      val hashA = generateHash(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      val hashB = generateHash(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      val hashEquality:Boolean = java.util.Arrays.equals(hashA, hashB)
      hashEquality should be(true)
    }

    it ("should be able to encode and decode to base64"){
      val HASH_BYTE_SIZE = 64 // 512 bits
      val SALT_BYTE_SIZE = 64 // 512 bits
      val PBKDF2_ITERATIONS = 20000
      val password="hello world"
      val salt = generateSeed(SALT_BYTE_SIZE)
      val originalHash = generateHash(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      val encodedHash:String = Base64.getEncoder().encodeToString(originalHash)
      val decodedHash:Array[Byte] = Base64.getDecoder().decode(encodedHash)
      val hashEquality:Boolean = java.util.Arrays.equals(originalHash, decodedHash)
      hashEquality should be(true)
    }
  }

  def generateSeed(size: Int):Array[Byte] = {
    val salt:Array[Byte] = new Array[Byte](size)
    val random = new SecureRandom()
    random.nextBytes(salt)
    return salt
  }

  def generateHash(password: String,
    salt: Array[Byte],
    numIterations: Int,
    hashByteSize: Int):Array[Byte] = {
    val spec = new PBEKeySpec(password.toArray, salt, numIterations, hashByteSize)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
    val hash = skf.generateSecret(spec).getEncoded()
    return hash
  }
}
