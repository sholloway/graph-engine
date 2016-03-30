# Todo List for MVP
- - -
The list of stuff that must be accomplished for version 0.1.0.

## Features
The list of features that version 0.1.0 shall be composed of.
* [ ] 0MQ Protocol
* [ ] Akka based concurrent statement execution.
* [ ] Internal Neo4J DSL
* [ ] Export a dataset to a file.
* [ ] Import a dataset from a file.


## Supporting Tasks
The list of general tasks that must be completed for version 0.1.0 to be considered done.
* [X] Add both element IDs to the response on the FindAssociationById command.
* [ ] Implement finding associations on a given Element.
* [ ] Return the count of elements in a dataset.
* [ ] Return the full list of elements and associations with pagenation.
* [ ] Neo4J Commands Refactor: Pull excluded list values (mid, dsId, etc...) into the parent class.
* [ ] Engine Refactor: Add a guard to withField method to check for empty spaces.
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
