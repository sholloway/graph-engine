#!/bin/bash

cd target/scala-2.11

java -cp machine-engine-assembly-0.1.0-SNAPSHOT-deps.jar:machine-engine-assembly-0.1.0-SNAPSHOT.jar \
  org.machine.engine.Main
