package org.machine.engine.graph.nodes

/** A person who interacts with the Engine system.
*
* @constructor Creates a new User.
* @param id The user's unique identifier. A type IV UUID.
* @param firstName The user's given name.
* @param lastName The user's family name.
* @param userName The user's prompted identifier. Used for authentication.
* @param emailAddress The user's recovery email address.
* @param creationTime When the element was created.
* @param lastModifiedTime When the element was last edited
* @param
*/
case class User(val id: String,
  val firstName: String,
  val lastName: String,
  val userName: String,
  val emailAddress: String,
  val creationTime: String,
  val lastModifiedTime: String
){
  override def toString():String = {
    val str = s"""
    | User
    | ID: $id
    | First Name: $firstName
    | Last Name: $lastName
    | User Name: $userName
    | Email Address: $emailAddress
    | Creation Time: $creationTime
    | Last Modified Time: $lastModifiedTime
    """.stripMargin
    return str
  }
}
