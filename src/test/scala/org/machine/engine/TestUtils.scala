package org.machine.engine

object TestUtils{
  /** Replaces all white space with just a single space.
  */
  def normalize(s: String): String = s.replaceAll("\\s+", " ")
}
