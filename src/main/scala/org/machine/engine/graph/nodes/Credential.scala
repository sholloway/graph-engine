package org.machine.engine.graph.nodes

/**

*/
case class Credential(val userId: String,
  val id: String,
  val passwordHash: String,
  val passwordSalt: String,
  val hashIterationCount: Int,
  val creationTime: Long,
  val lastModifiedTime: Long
){
  override def toString():String = {
    val str = s"""
      ID: $id,
      Password Hash: $passwordHash,
      Password Salt: $passwordSalt,
      Hash Iteration Count: $hashIterationCount,
      Creation TIme: $creationTime,
      Last Modified Time: $lastModifiedTime
    """.stripMargin
    return str;
  }
}
