# Graph Engine [![Build Status](https://travis-ci.org/sholloway/graph-engine.svg?branch=dev)](https://travis-ci.org/sholloway/graph-engine) [![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
- - -
A marriage of Scala, Akka and embedded Neo4J.

Status: Under active development working towards version 0.1.0. Not stable. Do not use.

## Getting Started
### Dependencies
* Install Scala and SBT.
```
brew install scala
brew install sbt
```

### Running the Engine
1. Run the tests.
```
sbt test
```

2. Build Dependencies Jar
```
sbt assemblyPackageDependency
```

3. Build Machine Engine Jar
```
sbt assembly
```

4. (Option 1) Start the Machine Engine via Node.js.
```
cd ./src/test/node
npm install
npm start
```

5. (Option 2) Start the Machine Engine as a stand alone service.
```
cd target/scala-2.11

java -cp machine-engine-assembly-0.1.0-SNAPSHOT-deps.jar:machine-engine-assembly-0.1.0-SNAPSHOT.jar \
  org.machine.engine.Main
```

6. The Web Socket Engine API is hosted at: http://localhost:2324/ws

### Integrating with the Engine
The engine has two channels for communication. A Web Socket endpoint for
business domain functions (i.e. manipulating graphs) and a STDIN/STDOUT endpoint
for doing administration (i.e. pausing, shutdown).

#### Communicating via Web Sockets
* ?

#### Communicating via STDIN/STDOUT
A complete example of communicating with the Engine via its STDIN endpoint
can be seen in the code base at src/test/node/index.js.
Supported Commands:
* SIGHUP: Shutdown command issued by the client.
* ENGINE_READY: Sent to STDOUT by the Engine once the engine is ready to receive
  commands on the WebSocket endpoint.

### SBT Tasks
| Name                            | Description                                                                                                                |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| tasks                           | Displays the list of available sbt tasks.                                                                                  |
| test                            | Compiles the project and runs the tests.                                                                                   |
| test-only *TestClassName        | Only runs a specific test.                                                                                                 |
| sbt "test-only *TestClassName"  | Only runs a specific test.                                                                                                 |
| assembly                        | Create executable jar.                                                                                                     |
| assemblyPackageDependency       | make a JAR file containing only the external dependencies                                                                  |
| cov                             | Compiles the project, runs the tests, generates the code coverage metric and then opens the report in the default browser. |
| doc                             | Generates the Scaladoc site.                                                                                               |
| dependencyUpdates               | Show a list of project dependencies that can be updated                                                                    |

## Related Resources
### Scala
* [Scala Cheatsheet](http://docs.scala-lang.org/cheatsheets/index.html)
* [sbt commands](http://www.scala-sbt.org/0.13/docs/Command-Line-Reference.html)
* [Scala Higher Kind Types](https://blogs.atlassian.com/2013/09/scala-types-of-a-higher-kind/)
* [Scala Quick Reference](http://www.tutorialspoint.com/scala/index.htm)
* [Scala Doc](http://docs.scala-lang.org/style/scaladoc.html)
* [Scala Code Coverage](https://github.com/scoverage/sbt-scoverage)
* [Apache Camel Stream](https://camel.apache.org/stream.html)
* [Akka Stdin Example](https://searler.github.io/scala/akka/camel/reactive/2015/01/11/Simple-Akka-Stream-Camel-Integration.html)
* [Akka REST DSL with Actors Example](https://spindance.com/reactive-rest-services-akka-http/)
* [Akka Sessions](https://github.com/softwaremill/akka-http-session)

### JSON
* [Lift JSON](https://github.com/lift/framework/tree/master/core/json)
* [Lift Community](https://groups.google.com/forum/#!forum/liftweb)
* [Lift API Docs](https://liftweb.net/api/31/api/net/liftweb/json/index.html)
* [Lift JSON Docs](https://app.assembla.com/spaces/liftweb/wiki/JSON_Support)

### Neo4J
* [Documentation](http://neo4j.com/docs/)
* [Java Docs](http://neo4j.com/docs/2.3.3/javadocs/)
* [Stored Procedures](https://github.com/neo4j-contrib/neo4j-apoc-procedures)

### Logging
* [TypeSafe LazyLogging](https://github.com/typesafehub/scala-logging)
* [Logback](http://logback.qos.ch/)

### Misc
* [Google Protocol Buffers](https://developers.google.com/protocol-buffers/)
* [TypeSafe Configuration](https://github.com/typesafehub/config)
* [I Am Done](https://github.com/imdone/imdone-core#metadata)
* [Semantic Versioning](http://semver.org/)
* [Web Socket Protocol](https://tools.ietf.org/html/rfc6455)
* [OKHttp Client](http://square.github.io/okhttp/)
