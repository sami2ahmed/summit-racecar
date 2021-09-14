# Realtime betting demo

There are two parts to this section of the demo:
1. a Java Spring app that produces streams records from a local dataset I've dervied from Kaggle to a topic: `racecarDemo` within Confluent Cloud
2. a CarEventReceiver app that consumes records from the `racecarDemo` topic, and pushes to web client over HTTP connection using server sent eventing
