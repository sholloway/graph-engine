package org.machine.engine.communication

import com.typesafe.config._
import org.scalatest._
import okhttp3.{Credentials, OkHttpClient, Request, RequestBody, Response, MediaType}
import org.machine.engine.Engine
import org.machine.engine.authentication.PasswordTools
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.GraphCommandOptions

object LoginHelper extends Matchers{
  import Neo4JHelper._
  private val config = ConfigFactory.load()
  private val userName = "tech49"
  private val password = "I love Sally."
  private val client = new OkHttpClient()
  private val jsonMediaType:okhttp3.MediaType = okhttp3.MediaType.parse("application/json; charset=utf-8");
  private val scheme = "http"
  private val host = config.getString("engine.communication.identity_service.host")
  private val port = config.getString("engine.communication.identity_service.port")
  val SESSION_HEADER:String = config.getString("engine.communication.webserver.session.header")

  def credentials(username: String, password: String):String = {
    return Credentials.basic(username, password)
  }

  def serviceCredentials():String = {
    val user:String = config.getString("engine.communication.identity_service.user")
    val pwd:String = config.getString("engine.communication.identity_service.password")
    return credentials(user, pwd);
  }

  /*Attempts to login and returns the JWT token.*/
  def login(serviceCreds:String, expectedHTTPStatus: Integer = 200):String = {
    val response = loginAttempt(userName, password,serviceCreds)
    response._1 should equal(expectedHTTPStatus)
    return response._3
  }

  /*
    Attempt to login with the provided user credentials. Converts the password
    to Base64 before calling the service.
    Returns: (responseCode, responseBody, jwt)
  */
  def loginAttempt(username: String,
    password: String,
    serviceCredentials:String):(Integer, String, String) = {
    val loginUrl = s"http://$host:$port/login"
    val encodedUserPwd = PasswordTools.strToBase64(password)
    val requestStr: String = s"""
    |{
    |   "userName": "${username}",
    |   "password": "${encodedUserPwd}"
    |}
    """.stripMargin
    val requestBody:RequestBody = RequestBody.create(jsonMediaType, requestStr)
    val loginRequest = new Request.Builder()
      .url(loginUrl)
      .header("Authorization", serviceCredentials)
      .post(requestBody)
      .build()
    val loginResponse = client.newCall(loginRequest).execute()
    val jwt = loginResponse.header("Set-Authorization")
    val responseCode = loginResponse.code()
    val responseBody = loginResponse.body().string()
    return (responseCode, responseBody, jwt)
  }

  def createUser(serviceCredentials: String):(Integer, String) = {
    val url:String = s"http://$host:$port/users"
    val requestStr:String = newUserRequest()
    val requestBody = RequestBody.create(jsonMediaType, requestStr)
    val request = new Request.Builder()
      .url(url)
      .post(requestBody)
      .header("Authorization", serviceCredentials)
      .build()
    val response = client.newCall(request).execute()
    val responseCode = response.code()
    val resultStr = response.body().string()
    return (responseCode, resultStr)
  }

  def deleteUser(serviceCredentials: String, userId:String) = {
    val deleteUserStmt:String = """
    | match (u:user) where u.mid={userId}
    | detach delete u
    """.stripMargin
    val params = GraphCommandOptions().addOption("userId", userId)
    run[String](Engine.getInstance.database,
      deleteUserStmt,
      params.toJavaMap,
      emptyResultProcessor[String])
  }

  def logout(jwt:String, credential:String, expectedHTTPStatus: Integer = 200):Unit = {
    val logoutUrl = s"http://$host:$port/logout"
    val logoutRequest = new Request.Builder()
      .url(logoutUrl)
      .header("Authorization", credential)
      .header(SESSION_HEADER, jwt)
      .build()
    val logoutResponse = client.newCall(logoutRequest).execute()
    logoutResponse.code() should equal(expectedHTTPStatus)
  }

  def attemptToGetProtectedResource(expectResponse:Int,
    jwt: String,
    credential:String
  ):Unit = {
    val url = s"http://$host:$port/protected"
    val logoutRequest = new Request.Builder()
      .url(url)
      .header("Authorization", credential)
      .header(SESSION_HEADER, jwt)
      .build()
      val logoutResponse = client.newCall(logoutRequest).execute()
      logoutResponse.code() should equal(expectResponse)
  }

  private def newUserRequest():String = {
    val encodedPwd = PasswordTools.strToBase64(password)
    val str = s"""
    {
      "emailAddress": "jharper@missioncontrol.com",
      "firstName": "Jack",
      "lastName": "Harper",
      "userName": "${userName}",
      "password": "${encodedPwd}"
    }
    """.stripMargin
    return str
  }

  private def createLoginRequest():RequestBody = {
    val encodedUserPwd = PasswordTools.strToBase64(password)
    val requestStr: String = s"""
    |{
    |   "userName": "$${userName}",
    |   "password": "${encodedUserPwd}"
    |}
    """.stripMargin
    return RequestBody.create(jsonMediaType, requestStr)
  }
}
