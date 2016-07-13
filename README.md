# Graph Engine
- - -
A marriage of 0MQ, Google Protocol Buffers, embedded Neo4J, Scala and Akka.

Status: Under active development working towards version 0.1.0. Not stable. Do not use.

## Getting Started
### Dependencies
* Install protocol buffer compiler
```
brew install protobuf
```

### SBT Tasks
| Name  | Description                                                                                                                |
|-------|----------------------------------------------------------------------------------------------------------------------------|
| tasks | Displays the list of available sbt tasks.                                                                                  |
| test  | Compiles the project and runs the tests.                                                                                   |
| test-only *TestClassName  | Only runs a specific test.                                                                             |
| sbt "test-only *TestClassName"  | Only runs a specific test.                                                                       |
| assembly | Create executable fat jar.                                                                                              |
| cov   | Compiles the project, runs the tests, generates the code coverage metric and then opens the report in the default browser. |
| doc   | Generates the Scaladoc site.                                                                                               |

## Related Resources
### Scala
* [Scala Cheatsheet](http://docs.scala-lang.org/cheatsheets/index.html)
* [sbt commands](http://www.scala-sbt.org/0.13/docs/Command-Line-Reference.html)
* [Scala Higher Kind Types](https://blogs.atlassian.com/2013/09/scala-types-of-a-higher-kind/)
* [Scala Quick Reference](http://www.tutorialspoint.com/scala/index.htm)
* [Scala Doc](http://docs.scala-lang.org/style/scaladoc.html)
* [Scala Code Coverage](https://github.com/scoverage/sbt-scoverage)

### ZeroMQ
* [ZeroMQ Node](https://github.com/JustinTulloss/zeromq.node)
* [ZeroMQ Socket Types](http://api.zeromq.org/2-1:zmq-socket)

### Neo4J
* [Documentation](http://neo4j.com/docs/)
* [Java Docs](http://neo4j.com/docs/2.3.3/javadocs/)
* [Stored Procedures](https://github.com/neo4j-contrib/neo4j-apoc-procedures)

### Misc
* [Google Protocol Buffers](https://developers.google.com/protocol-buffers/)
* [TypeSafe Configuration](https://github.com/typesafehub/config)
* [TypeSafe LazyLogging](https://github.com/typesafehub/scala-logging)
* [Logback](http://logback.qos.ch/)
* [I Am Done](https://github.com/imdone/imdone-core#metadata)
* [Semantic Versioning](http://semver.org/)
* [Web Socket Protocol](https://tools.ietf.org/html/rfc6455)
