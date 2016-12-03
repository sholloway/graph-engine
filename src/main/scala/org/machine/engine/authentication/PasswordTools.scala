package org.machine.engine.authentication
/*
What I need is ways to:
* Register a User
    * Fname, lastname, password.
    * Creates a User node.
    * Creates a Credential node.
    * Associates nodes to each other and the User to the System Space.
* Authenticate a User
* Change a User's Password.
*/
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
/** Set of helper functions for working with hashes and byte arrays for user authentication.
*/
object PasswordTools{
  /** Creates a salt of specified size using a secure random number generator.
  @param size: The length of the byte array that will contain the seed.
  */
  def generateSalt(size: Int):Array[Byte] = {
    val salt:Array[Byte] = new Array[Byte](size)
    val random = new SecureRandom()
    random.nextBytes(salt)
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
}
