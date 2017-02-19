package org.machine.engine.authentication

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom
import java.util.Base64
import java.util.Random;
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Set of helper functions for working with hashes and byte arrays for user
  authentication and identity management.
*/
object PasswordTools{
  def createRandomNumberGenerator(seed: Long):Random = {
    val generator:SecureRandom = SecureRandom.getInstance("SHA1PRNG");
    generator.setSeed(seed)
    return generator
  }

  /** Generates a session ID.
  @param size: The length of the byte array that
  will contain the session ID.
  */
  def generateSessionId(generator:Random, size: Int):Array[Byte] = {
      val number:Array[Byte] = new Array[Byte](size)
      generator.nextBytes(number)
      val sha:MessageDigest  = MessageDigest.getInstance("SHA-1");
      val digest:Array[Byte] =  sha.digest(number);
      return digest
  }

  /**
  Generate a seed by taking the bitwise OR of the current system time since Epoch
  and the available system memory.
  */
  def generateSeed():Long = {
    val seed = System.currentTimeMillis() | Runtime.getRuntime().freeMemory()
    return seed
  }


  /** Creates a salt of specified size using a secure random number generator.
  @param size: The length of the byte array that will contain the seed.
  */
  def generateSalt(generator: Random, size: Int):Array[Byte] = {
    val salt:Array[Byte] = new Array[Byte](size)
    generator.nextBytes(salt)
    return salt
  }

  /** Generates a one way 64 byte hash of a password.
  @param password: The secret to hash.
  @param salt: A piece of randomness to add to the password. Use the PasswordTools.generateSalt function.
  @param numIterations: The number of iterations to cycle the key derivation function.
  */
  def generateHash(password: String,
    salt: Array[Byte],
    numIterations: Int):Array[Byte] = {
    val hashByteSize = 64 // 512 bits
    val spec = new PBEKeySpec(password.toArray, salt, numIterations, hashByteSize)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
    val hash = skf.generateSecret(spec).getEncoded()
    return hash
  }

  /** Converts a byte array to a base64 encoded string.
  @param hash: The byte array to encode.
  */
  def hashToBase64(hash: Array[Byte]):String = {
    return Base64.getEncoder().encodeToString(hash)
  }

  /** Converts a base64 encoded string to a byte array.
  @param hashStr: The encoded string to convert.
  */
  def base64ToHash(hashStr: String):Array[Byte] = {
    return Base64.getDecoder().decode(hashStr)
  }

  /** Compares to byte arrays.
  @param hashA: The first array to compare.
  @param hashB: The second array to compare.
  */
  def compare(hashA: Array[Byte], hashB: Array[Byte]):Boolean = {
    return java.util.Arrays.equals(hashA, hashB)
  }

  def byteArrayToHexStr(bytes: Array[Byte]):String = {
    return bytes.map("%02X" format _).mkString
  }
}
