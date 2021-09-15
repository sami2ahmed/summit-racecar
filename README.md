# Realtime (racecar) betting demo with Confluent Cloud 

The purpose of this project is to show how we can easily create a realtime betting platform using Confluent Cloud and Ksqldb.

This example is based on a few datasets combined from Kaggle's [Formula One dataset]('https://www.kaggle.com/cjgdev/formula-1-race-data-19502017')

At a high level, there are four parts to this demo:
1. springboot (java) to populate racecar data and drive frontend with realtime events 
2. Vue.js to render the race  
3. Confluent Cloud + ksqldb (hosted stream processing in Confluent) performing data transformations as the data flows into a Confluent Cloud cluster
4. node.js to simulate betting on the race 

# Prerequisites
1. Basic cluster in Confluent Cloud
2. Confluent Cloud CLI 
3. npm 
3.5. npm install kafkajs, json-schema-faker and faker 
4. java11
5. vue.js 
6. maven 
7. jq

# Demo architecture 
![high level arch](https://github.com/sami2ahmed/summit-racecar/blob/master/img/summit_arch.png) 
![confluent gen arch](https://github.com/sami2ahmed/summit-racecar/blob/master/img/auto_gen_1.png) 
![confluent gen arch2](https://github.com/sami2ahmed/summit-racecar/blob/master/img/auto_gen_2.png)
![ksql flow gen 1](https://github.com/sami2ahmed/summit-racecar/blob/master/img/ksql_flow_1.png)
![ksql flow gen 2](https://github.com/sami2ahmed/summit-racecar/blob/master/img/ksql_flow_2.png)



# Running the demo 

## setting up the Confluent Cloud components 
ccloud login --save \
chmod +x setup.sh \
./setup.sh \
make sure all components (including ksql queries) come up without error before moving on to deploying the race \

You will know this was successful when you see the ksql queries loaded and data in the odds-landing topic (make sure you're looking from the beginning of the topic!) \
![queries](https://github.com/sami2ahmed/summit-racecar/blob/master/img/ksqlqueries.png)
![odds-landing](https://github.com/sami2ahmed/summit-racecar/blob/master/img/oddslanding.png)

## to setup the frontend
git clone this repo and in a terminal run the frontend.sh file from the summit-racecar directory i.e. 
1. `chmod +x frontend.sh`
2. `bash frontend.sh` 

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
![UI](https://github.com/sami2ahmed/summit-racecar/blob/master/img/UI.png)

Now to establish the server sent events (SSE) connection, ensure that port within the application.yml nested in the race-car-events module is set to the same port as the local UI endpoint (8080 in this case). 

Importantly, you will also notice an application.properties file in the laptime-produce module i.e. (`java/laptime-producer/src/main/resources/application.properties`). Please configure the 4 environment variables so that this properties file is referencing your Confluent basic cluster, for example in my local application.properties I have: 

```
BOOTSTRAP_SERVERS=pkc-ef9nm.us-east-2.aws.confluent.cloud:9092
SECURITY_PROTOCOL=SASL_SSL
SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule   required username='NOTREALYES4FAKER'   password='NOTREALBKDFLK1+nlOVC+NFeYZ9BpHDcsnQuhdgJeMaQhlHChlw/5+Cgsgl4lEnP';
SASL_MECHANISM=PLAIN
```
Please also configure those same four variables within the yaml file for the race-car-events module i.e. (`java/race-car-events/src/main/resources/application.yml`). Leave the port set to 8080. 

## To run the SSE (RaceCarEventsApplication.java)
open up another terminal window and run the sse.sh file i.e. 
1. `chmod +x sse.sh`
2. `bash sse.sh` 

You should see a Netty started on 8080.

## Now we need to give the the sse access to the cluster through consumer group ACL. 
1. `chmod +x SseConsumerAcl.sh`
2. `bash SseConsumerAcl.sh`   
  
Go back to the UI, and hit the "connect" button. You should see a timestamp entry in the UI. 
In the RaceCarEventsApplication terminal output, you will see an "UNKNOWN_TOPIC" error which is expected. The topics will been created by the RacecarProducer.sh (RacecarApp.java) in our next step.

We can now produce data from our client to server now that the connection has been initiated and established by the server. When you launch the RacecarApp, you should see racecar data getting populated into the UI and also into the Confluent Cloud topic. 
Now that topics are created, in the RaceCarEventsApplication output, you should now also see a consumer group subscribe to racecarDemo topic, some output like: `Consumer clientId=car-consumer-1-8d93a253-34ca-4f2a-910f-86ed47487d7b, groupId=1] Subscribed to topic(s): racecarDemo`.  
  
## Open up another new terminal to launch the laptime-produce (RacecarApp.java)
1. `chmod +x RacecarProducer.sh`
2. `bash RacecarProducer.sh` 

You should see the following in Confluent Cloud and the UI: 
![CCloud](https://github.com/sami2ahmed/summit-racecar/blob/master/img/CCloud.png)
![UI with data](https://github.com/sami2ahmed/summit-racecar/blob/master/img/UI_wdata.png)

In running the RacecarApp, if any of the Immutable data models are giving you guff (saying they cannot be found on import etc.) try reloading all maven projects. Also please spin up the frontend before the backend to avoid port conflicts. 

You can control + c in each terminal window to spin down the UI and backend (both the SSE and producer) when you are done. 
  
## Simulating bets 

Once you've kicked off your race, you can see the race events flowing through in either the racecarDemo topic (select the messages tab) or by querying the racecardemo stream in ksqlDB: \
`select * from racecardemo emit changes` \
\
You can see the status of the race (who is in which position) by querying the racecarstatus table in ksqlDb \
`select * from racecarstatus emit changes` 

Simulating bets will cause your odds calculations to change. In order to simulate bets for a given user, please run: \
`node/bet-producer.js <bootstrap-server> <api-key> <api-secret> <racecardriver you want to bet on> <# of bets to simulate>` \
  you will be able to see the changing odds by running queries against the odds ktable 
  
  
###### ksqlDB queries to run to see the action!   

to see the current status of the betting: \
`select * from betaggregation [emit changes]` \
\
to see the current staus of odds: \
`select * from odds [emit changes]` 

