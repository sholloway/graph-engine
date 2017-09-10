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
    val vertices = List(
      "element_definition",
      "property_definition",
      "element",
      "data_set",
      "user",
      "credential",
      "session"
    )
    vertices.foreach(vertex => {
      run(Engine.getInstance.database,
        s"match (v:${vertex}) detach delete v",
        map,
        emptyResultProcessor[String])
    })
  }
}
