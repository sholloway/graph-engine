package org.machine.engine.viz

import org.scalatest._
import org.scalatest.mock._

import com.typesafe.config._
import java.io.File;
import java.io.IOException;
import org.machine.engine.Engine

/*
This test assumes that another test has run and produced a database.
*/
class GraphVizOfEntireDatabaseSpec extends FunSpecLike
  with Matchers with BeforeAndAfterAll{
  private val config = ConfigFactory.load()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null

  override def beforeAll(){
    Engine.shutdown
    engine = Engine.getInstance
  }

  override def afterAll(){
    Engine.shutdown
  }

  describe("Visualize all Connections"){
    /*
    This test is intended to aid in debuging and should not be
    run as part of continuous integration build.
    */
    ignore ("should generate a database visualization"){
      GraphVizHelper.visualize(engine.database,
        s"${GraphVizHelper.wd}/viz",
        "temp.dot")
    }
  }
}
