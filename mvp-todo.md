# Todo List for MVP
- - -
The list of stuff that must be accomplished for version 0.1.0.

## Features
The list of features that version 0.1.0 shall be composed of.
* [ ] 0MQ Protocol
* [ ] Akka based concurrent statement execution.
* [X] Internal Neo4J DSL
* [ ] Export a dataset to a file.
* [ ] Import a dataset from a file.

## Supporting Tasks
The list of general tasks that must be completed for version 0.1.0 to be considered done.
* [X] Add both element IDs to the response on the FindAssociationById command.
* [X] Implement finding associations on a given Element.
* [ ] Engine Refactor: Add a guard to withField method to check for empty spaces.
* [ ] Return the count of elements in a dataset.
* [ ] Return the full list of elements and associations with pagenation.
* [ ] Neo4J Commands Refactor: Pull excluded list values (mid, dsId, etc...) into the parent class.
* [ ] Implement 0MQ security model.
* [ ] Add data indexes to Element mid & name.
* [ ] Add data indexes to ElementDefinition mid & name.
* [ ] Add data indexes to Association associationId.
* [ ] Code Coverage above 80%.
* [ ] Travis.CI integration.
* [ ] Scaladoc documentation for all classes.
* [ ] Run code/tests on multiple computers.
* [ ] Add license file and license headers.
* [ ] External review of DSL.
* [ ] Leverage sbt assembly to create an executable self contained jar.
* [ ] Host scaladocs. Potentially leverage sbt plugin: sbt-ghpages
* [ ] Remove all warnings
* [ ] Refactor GraphDSL.withProperty to use a case Class or enum for ptype.
* [ ] Add @throws to functions that throw exceptions.
* [ ] Refactor FindAssociationById to work like FindOutboundAssociationsByElementId
* [ ] Refactor FindElement commands to work like FindOutboundAssociationsByElementId
* [X] Add GraphDSL.removeOutboundAssociations() for a given element.
* [ ] Leverage https://www.scalacheck.org/ for testing the API.
* [ ] Re-write the README.md to use a table for the sbt tasks.
* [ ] Consider using https://github.com/os72/protoc-jar or something similar to remove protoc dependency.

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
* Do we use Java Protocol Buffers or ScalaPB? ScalaPB will force Protobuf2 (No maps). The default protoc install with hombrew also does not support proto3.
* Use Scalacheck to generate messages from both JS and Scala and compare?
* Use Scalacheck to make sure there isn't an invalid combination of Inbound message options -> Command?
* How should we model GraphCommandOptions from the API perspective?
  * Protobuf2 Does not support Maps.
  * Protobuf3 Has maps, but I'm not going to be able to do Map[String, Any]
  * A field has name, type, value. What should the value be?
  * Give this a shot:
    * Leverage Google's Java implementation for proto3.
    * Create a custom SBT task to run the compiler and dump to a package.
    * Should have a README in the package that makes it clear not to edit generated files.
    * Isolate the code that works with the proto code.
