#!/bin/bash

cd java/laptime-producer
export JAVA_HOME='/Library/Java/JavaVirtualMachines/jdk-11.0.8.jdk/Contents/Home'
mvn package -U
mvn org.springframework.boot:spring-boot-maven-plugin:run
