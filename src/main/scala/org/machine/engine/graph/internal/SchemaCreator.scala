package org.machine.engine.graph.internal

import com.typesafe.scalalogging._
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.neo4j.graphdb._
import scala.collection.JavaConversions._
import Neo4JHelper._

object SchemaCreator extends StrictLogging{
  import Neo4JHelper._

  private val ListConstraintsStmt = "CALL db.constraints()"
  private val Description = "description"
  private val indicies = scala.collection.immutable.Seq()

  //Note: Creating a constraint also creates an index on it.
  private val constraints = scala.collection.immutable.Seq(
    "CREATE CONSTRAINT ON (u:user) ASSERT u.mid IS UNIQUE",
    "CREATE CONSTRAINT ON (c:credential) ASSERT c.mid IS UNIQUE",
    "CREATE CONSTRAINT ON (d:data_set) ASSERT d.mid IS UNIQUE",
    "CREATE CONSTRAINT ON (s:internal_system_space) ASSERT s.mid IS UNIQUE",
    "CREATE CONSTRAINT ON (ed:element_definition) ASSERT ed.mid IS UNIQUE",
    "CREATE CONSTRAINT ON (pd:property_definition) ASSERT pd.mid IS UNIQUE",
    "CREATE CONSTRAINT ON (e:element) ASSERT e.mid IS UNIQUE",
    "CREATE CONSTRAINT ON (s:session) ASSERT s.session IS UNIQUE",

    "CREATE CONSTRAINT ON (u:user) ASSERT u.username IS UNIQUE",
    "CREATE CONSTRAINT ON (d:data_set) ASSERT d.name IS UNIQUE",
    "CREATE CONSTRAINT ON (s:internal_system_space) ASSERT s.name IS UNIQUE",
    "CREATE CONSTRAINT ON (ed:element_definition) ASSERT ed.name IS UNIQUE"
  )

  private val createSystemSpaceStmt = """
  |create (ss:internal_system_space{
  | mid:{mid},
  | name:{name},
  | creation_time:timestamp(),
  | last_modified_time:timestamp()
  |})
  """.stripMargin

  def createPropertyGraphDataModel(db: GraphDatabaseService) = {
    logger.debug("Establishing the data schema.")
    if(!dataModelEstablished(db)){
      transaction(db, (graphDB: GraphDatabaseService) =>{
        createConstraints(db)
        createIndices(db)
      })
      systemSpace(db)
      defineSystemElementDefinitions(db)
    }
  }

  private def dataModelEstablished(db: GraphDatabaseService):Boolean = {
    val foundConstraints:Array[String] = query[String](db,
      ListConstraintsStmt,
      null,
      { (results, record) =>
          val constrainDescription:String = record.get(Description).toString()
          results += constrainDescription
        }
    )
    return foundConstraints.length >= constraints.length
  }

  private def createIndices(db: GraphDatabaseService) = {
    logger.debug("Creating all the graph indicies.")
    indicies.foreach(indexStmt => {
      run[String](db, indexStmt, null, emptyResultProcessor[String])
    })
  }

  private def createConstraints(db: GraphDatabaseService) = {
    logger.debug("Creating the graph constraints.")
    constraints.foreach(constraintStmt => {
      run[String](db, constraintStmt, null, emptyResultProcessor[String])
    })
  }

  private def systemSpace(db: GraphDatabaseService) = {
    logger.debug("Creating the graph's system space.")
    val params = Map("mid"->uuid, "name"->"System Space")
    transaction(db, (graphDB: GraphDatabaseService) =>{
      run[String](db, createSystemSpaceStmt, params, emptyResultProcessor[String])
    })
  }

  private def defineSystemElementDefinitions(db: GraphDatabaseService) = {
    logger.debug("Creating the system's element definitions.")
    //Reserving this for later...
    //Create Notes, Wikipedia, Video, etc... system level Elemend Definitions here.
    //Perhaps we need a data driven way of doing this, like setting up firewall rules.
    //Like having "sets" of ElementDefinitions that could be shared.
  }
}
