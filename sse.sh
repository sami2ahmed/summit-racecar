#!/bin/bash

export JAVA_HOME='/Library/Java/JavaVirtualMachines/jdk-11.0.8.jdk/Contents/Home'
mvn install
cd java/race-car-events
mvn org.springframework.boot:spring-boot-maven-plugin:run
