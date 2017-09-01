package org.machine.engine

import org.machine.engine.graph.Neo4JHelper

object TestUtils{
  import Neo4JHelper._

  /** Replaces all white space with just a single space.
  */
  def normalize(s: String): String = s.replaceAll("\\s+", " ")

  /*
  WARNING: Deletes all data in an attached Neo4J database while preserving
  the system space and user space nodes.
  Intended to only be used by tests.
  */
  def perge = {
    val map = new java.util.HashMap[java.lang.String, Object]()
    val deleteStatements = List(
      "match (ed:element_definition) detach delete ed",
      "match (pd:property_definition) detach delete pd",
      "match (e:element) detach delete e",
      "match (ds:internal_data_set) detach delete ds",
      "match (u:user) detach delete u",
      "match (uc:credential) detach delete uc",
      "match (s:session) detach delete s"
    )
    deleteStatements.foreach(statement => {
      run(Engine.getInstance.database,
        statement,
        map,
        emptyResultProcessor[String])
    })
  }
}
