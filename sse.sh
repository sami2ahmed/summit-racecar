#!/bin/bash

cd java
export JAVA_HOME='/Library/Java/JavaVirtualMachines/jdk-11.0.8.jdk/Contents/Home'
mvn install
cd java/race-car-events
