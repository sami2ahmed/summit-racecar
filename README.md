# Realtime betting demo 

The purpose of this project is to show how we can easily create a realtime betting platform using Confluent Cloud, vue.js, and ksqlDB. 

This example is based on a few datasets combined from Kaggle's [Formula One dataset](https://www.kaggle.com/cjgdev/formula-1-race-data-19502017)

There are two parts to this example:
1. a Java Spring app that produces streams records from a local dataset I've dervied from Kaggle to a topic: `racecarDemo` within Confluent Cloud
2. a CarEventReceiver app that consumes records from the `racecarDemo` topic, and pushes to web client (terminal for now) over HTTP connection using server side eventing.  

To run this in your environment, you'll want to edit the values in the following properties file: 
 - `kafka-iris-data/src/main/resources/application.properties`

As well as edit your environment variables within your IDE (I used IntelliJ so it's within edit configurations > environment variables) to get all the required data to connect to your Confluent Cloud instance (bootstrap-servers etc.)   


The model project was created using [Immutables](https://immutables.github.io/) which will require [configuration of your ide](https://immutables.github.io/apt.html)