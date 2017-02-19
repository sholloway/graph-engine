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
    val delete_element_definitions = "match (ed:element_definition) detach delete ed"
    val delete_property_definitions = "match (pd:property_definition) detach delete pd"
    val delete_elements = "match (e:element) detach delete e"
    val delete_datasets = "match (ds:internal_data_set) detach delete ds"

    val map = new java.util.HashMap[java.lang.String, Object]()
    run(Engine.getInstance.database, delete_element_definitions, map, emptyResultProcessor[String])
    run(Engine.getInstance.database, delete_property_definitions, map, emptyResultProcessor[String])
    run(Engine.getInstance.database, delete_elements, map, emptyResultProcessor[String])
    run(Engine.getInstance.database, delete_datasets, map, emptyResultProcessor[String])
  }
}
