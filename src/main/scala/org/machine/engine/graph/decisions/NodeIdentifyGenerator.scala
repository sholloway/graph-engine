package org.machine.engine.graph.decisions

object NodeIdentityGenerator{
  private var counter: Short = 0
  private val incr: Short = 1

  def id:Short = {
    counter = (counter + incr).toShort
    return counter
  }

  def reset{
    counter = 0
  }
}
