# Realtime (racecar) betting demo with Confluent Cloud 

The purpose of this project is to show how we can easily create a realtime betting platform using Confluent Cloud, Vue.js, Springboot, and Confluent's ksqlDB.

This example is based on a few datasets combined from Kaggle's [Formula One dataset]('https://www.kaggle.com/cjgdev/formula-1-race-data-19502017')

At a high level, there are three parts to this demo:
1. springboot backend
2. Vue.js frontend 
3. Confluent Cloud + ksqldb (hosted stream processing in Confluent) performing data transformations as the data flows into a Confluent Cloud cluster.

# Prerequisites
1. Basic cluster in Confluent Cloud
2. Confluent Cloud CLI 
3. npm
3.5. npm install kafkajs, json-schema-faker and faker 
4. java11
5. vue.js 
6. maven 

# Demo architecture 

# Running the demo 
To setup the Confluent Cloud cluster, you need to give the springboot service access to produce to the cluster. In a terminal run the following commands: 
ccloud login
ccloud kafka cluster list
ccloud kafka cluster use <enter lkc for basic cluster here>

## Consumer group access for input and internal topics by application.id prefix
`ccloud kafka acl create --allow --service-account <service account id> --operation READ --operation delete --operation describe --consumer-group car-consumer --prefix`

## Give service account access to all topics in cluster
`ccloud kafka acl create --allow --service-account <service account id> --operation READ --operation delete --operation describe --topic '*'`

*note on the above, the service account id is a 7 digit id. 

to setup the frontend, git clone this repo and in a terminal run the following commands: 
cd /frontend
npm install 
npm run serve 

```
you may receive this error, please ignore and continue: 
ERROR in /Users/samiahmed/IdeaProjects/summit_racecar_demo/frontend/src/App.vue(83,9):
83:9 Type 'string' is not assignable to type '"json" | "plain" | MessageFormatter<any> | undefined'.
    81 |       client = this.$sse.create({
    82 |         url: this.url,
  > 83 |         format: this.format,
       |         ^
    84 |         color: '#00000'
    85 |       //  includeCredentials: this.includeCredentials
    86 |       })
Version: typescript 3.9.9
```

open browser window, copy paste local endpoint app spins up on, for example: 
`http://localhost:8080/`

you should now see the UI in the browser: 

Now to establish the SSE connection, ensure that port within the application.yml nested in the race-car-events module is set to the same port as the local UI endpoint (8080 in this case). 

Importantly, you will also notice an application.properties file in the laptime-produce module i.e. (`backend/laptime-producer/src/main/resources/application.properties`). Please configure the 4 environment variables so that this properties file is referencing your Confluent basic cluster, for example in my IntelliJ configurations I have: 

```
BOOTSTRAP_SERVERS=pkc-ef9nm.us-east-2.aws.confluent.cloud:9092
SECURITY_PROTOCOL=SASL_SSL
SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule   required username='NOTREALYES4FAKER'   password='NOTREALBKDFLK1+nlOVC+NFeYZ9BpHDcsnQuhdgJeMaQhlHChlw/5+Cgsgl4lEnP';
SASL_MECHANISM=PLAIN
```
## To run the SSE (RaceCarEventsApplication.java), open up a new terminal and run the <tbd>.sh file 
export JAVA_HOME=`/usr/libexec/java_home -v 1.11` 
cd summit-racecar/backend 
mvn package -U 
cd race-car-events  
mvn package -U 
mvn org.springframework.boot:spring-boot-maven-plugin:run

You should see a Netty started on 8080.
Go back to the UI, and hit the "connect" button. You should see a timestamp entry in the UI. 
In the RaceCarEventsApplication terminal output, you will see an "UNKNOWN_TOPIC" error which is expected. The topics will been created by the RacecarApp in our next step.

We can now produce data from our client to server now that the connection has been initiated and established by the server. Launch the RacecarApp, you should momentarily see racecar data getting populated into the UI and also into the Confluent Cloud topic. 
Now that topics are created, in the RaceCarEventsApplication output, you should now also see a consumer group subscribe to racecarDemo topic, some output like: `Consumer clientId=car-consumer-1-8e93e253-34ca-4e2e-910f-86ed47487d7b, groupId=1] Subscribed to topic(s): racecarDemo`.  
  
## Open up another new terminal, to start laptime-produce (RacecarApp.java), run the <tbd2>.sh file
cd summit-racecar/backend/laptime-producer
export JAVA_HOME=`/usr/libexec/java_home -v 1.11` 
mvn package -U 
mvn org.springframework.boot:spring-boot-maven-plugin:run 


In running the RacecarApp, if any of the Immutable data models are giving you guff (saying they cannot be found on import etc.) try reloading all maven projects. Also please spin up the frontend before the backend to avoid port conflicts. 

You can control + c in each terminal window to spin down the UI and backend (both the SSE and producer) when you are done. 
