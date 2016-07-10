# Todo List for MVP
- - -
The list of stuff that must be accomplished for version 0.1.0.

## Features
The list of features that version 0.1.0 shall be composed of.
* [X] WebSocket Protocol
* [ ] Akka based concurrent statement execution.
* [X] Internal Neo4J DSL
* [ ] Export a dataset to a file.
* [ ] Import a dataset from a file.
* [X] Internal Decision Tree

## Supporting Tasks
The list of general tasks that must be completed for version 0.1.0 to be considered done.
* [ ] Leverage sbt assembly to create an executable self contained jar.
* [ ] Create Main class.
* [ ] Create WebSocket shutdown command to allow the Master client to shut the server down.
  * Note: This should only be allowed for initiating client.
* [ ] Fix Akka Streams memory leak. (http://www.cubrid.org/blog/dev-platform/how-to-monitor-java-garbage-collection/)
* [ ] Change Akka Streams to be asynchronous.
* [ ] Change Akka Streams to leverage the worker pool pattern.
* [ ] Put creationTime & lastModifiedTime on ElementDefintion.
* [ ] Add the concept of a User to the data model. This will require the DSL change.
* [ ] Add on demand SVG file of dataset.
* [ ] Add on demand SVG file of decision tree.
* [ ] Add TLS support for WebSocket connection.
* [ ] Register the Neo4J shutdown with the JVM shutdown like in the example.
* [ ] Upgrade to Neo4J 3.
* [ ] Travis.CI integration. (http://www.scala-sbt.org/0.13/docs/Travis-CI-with-sbt.html)
* [ ] Document WebSocket protocol.
* [ ] Scaladoc documentation for all classes.
* [ ] Host scaladocs. Potentially leverage sbt plugin: sbt-ghpages
* [ ] Engine Refactor: Add a guard to withField method to check for empty spaces.
* [ ] Add data indexes to Element mid & name.
* [ ] Add data indexes to ElementDefinition mid & name.
* [ ] Add data indexes to Association associationId.
* [ ] Refactor FindAssociationById to work like FindOutboundAssociationsByElementId
* [ ] Refactor FindElement commands to work like FindOutboundAssociationsByElementId
* [ ] Neo4J Commands Refactor: Pull excluded list values (mid, dsId, etc...) into the parent class or package file.
* [ ] Add a search for elements by field command.
* [ ] Add a search for element definition by field command.
* [ ] Return the count of elements in a dataset.
* [ ] Return the full list of elements and associations with pagenation.
* [ ] Run code/tests on multiple computers.
* [ ] External review of DSL.
* [ ] Refactor GraphDSL.withProperty to use a case Class or enum for ptype.
* [ ] Add @throws to functions that throw exceptions.
* [ ] Leverage https://www.scalacheck.org/ for testing the API.
* [ ] Consider using https://github.com/os72/protoc-jar or something similar to remove protoc dependency.
* [ ] Update diagrams to show concurrency. (ActorSystem, ActorRef, EventBus...)
* [X] Add license file and license headers.
* [X] Define outbound protocol contract.
* [X] Implement finding associations on a given Element.
* [X] Code Coverage above 80%.
* [X] Remove all warnings
* [X] Add GraphDSL.removeOutboundAssociations() for a given element.
* [X] Re-write the README.md to use a table for the sbt tasks.
* [X] Upgrade Akka version to 2.4.3
* [X] Have the logger write to files.
* [X] Add both element IDs to the response on the FindAssociationById command.
* [X] Replace my Logger stub with SL4FJ. http://www.slf4j.org/manual.html
* [X] Change the protobuf package to be org.machine.engine.messages

## Notes
On startup:
1. Check for config file.
  * If no file found error out.
  * If file found load config.
2. From config attempt to open DB.
  * If no DB found, error out.
3. Validate DB:
  * Check for System Space
    * If none found, create it.

## Decision Points
* Use Scalacheck to generate messages from both JS and Scala and compare?
* Use Scalacheck to make sure there isn't an invalid combination of Inbound message options -> Command?
