#!/bin/bash


##################################################
# Create a new environment and specify it as the default
##################################################

ENVIRONMENT_NAME="racecar-demo"
echo -e "\n# Create a new Confluent Cloud environment $ENVIRONMENT_NAME"
echo "ccloud environment create $ENVIRONMENT_NAME -o json"
OUTPUT=$(ccloud environment create $ENVIRONMENT_NAME -o json)
if [[ $? != 0 ]]; then
  echo "ERROR: Failed to create environment $ENVIRONMENT_NAME. Please troubleshoot (maybe run ./cleanup.sh) and run again"
  exit 1
fi
echo "$OUTPUT" | jq .
ENVIRONMENT=$(echo "$OUTPUT" | jq -r ".id")
#echo $ENVIRONMENT

echo -e "\n# Specify $ENVIRONMENT as the active environment"
echo "ccloud environment use $ENVIRONMENT"
ccloud environment use $ENVIRONMENT

##################################################
# Create a new Kafka cluster and specify it as the default
##################################################

CLUSTER_NAME="${CLUSTER_NAME:-demo-kafka-cluster}"
CLUSTER_CLOUD="${CLUSTER_CLOUD:-aws}"
CLUSTER_REGION="${CLUSTER_REGION:-us-west-2}"
echo -e "\n# Create a new Confluent Cloud cluster $CLUSTER_NAME"
echo "ccloud kafka cluster create $CLUSTER_NAME --cloud $CLUSTER_CLOUD --region $CLUSTER_REGION"
OUTPUT=$(ccloud kafka cluster create $CLUSTER_NAME --cloud $CLUSTER_CLOUD --region $CLUSTER_REGION)
status=$?
echo "$OUTPUT"
if [[ $status != 0 ]]; then
  echo "ERROR: Failed to create Kafka cluster $CLUSTER_NAME. Please troubleshoot and run again"
  exit 1
fi
CLUSTER=$(echo "$OUTPUT" | grep '| Id' | awk '{print $4;}')

echo -e "\n# Specify $CLUSTER as the active Kafka cluster"
echo "ccloud kafka cluster use $CLUSTER"
ccloud kafka cluster use $CLUSTER

BOOTSTRAP_SERVERS=$(ccloud kafka cluster describe $CLUSTER -o json | jq -r ".endpoint" | cut -c 12-)
#echo "BOOTSTRAP_SERVERS: $BOOTSTRAP_SERVERS"

##################################################
# Create a user key/secret pair and specify it as the default
##################################################

echo -e "\n# Create a new API key for user"
echo "ccloud api-key create --description \"Demo credentials\" --resource $CLUSTER -o json"
OUTPUT=$(ccloud api-key create --description "Demo credentials" --resource $CLUSTER -o json)
status=$?
if [[ $status != 0 ]]; then
  echo "ERROR: Failed to create an API key.  Please troubleshoot and run again"
  exit 1
fi
echo "$OUTPUT" | jq .

API_KEY=$(echo "$OUTPUT" | jq -r ".key")
echo -e "\n# Associate the API key $API_KEY to the Kafka cluster $CLUSTER"
echo "ccloud api-key use $API_KEY --resource $CLUSTER"
ccloud api-key use $API_KEY --resource $CLUSTER

sleep 100


##################################################
# Create necessary topics
##################################################

TOPIC1="odds-landing"

echo -e "\n# Create a new Kafka topic for initial odds data $TOPIC1"
echo "ccloud kafka topic create $TOPIC1"
ccloud kafka topic create $TOPIC1
status=$?
if [[ $status != 0 ]]; then
  echo "ERROR: Failed to create topic $TOPIC1. Please troubleshoot and run again"
  exit 1
fi

TOPIC2="bet-landing"

echo -e "\n# Create a new Kafka topic for landing bet data $TOPIC2"
echo "ccloud kafka topic create $TOPIC2"
ccloud kafka topic create $TOPIC2
status=$?
if [[ $status != 0 ]]; then
  echo "ERROR: Failed to create topic $TOPIC2. Please troubleshoot and run again"
  exit 1
fi

##################################################
# Create service account for odds-landing producer 
##################################################

echo -e "\n# Create a new service account"
RANDOM_NUM=$((1 + RANDOM % 1000000))
SERVICE_NAME="odds-app-$RANDOM_NUM"
echo "ccloud service-account create $SERVICE_NAME --description $SERVICE_NAME -o json"
OUTPUT=$(ccloud service-account create $SERVICE_NAME --description $SERVICE_NAME  -o json)
echo "$OUTPUT" | jq .
SERVICE_ACCOUNT_ID=$(echo "$OUTPUT" | jq -r ".id")

echo -e "\n# Create an API key and secret for the service account $SERVICE_ACCOUNT_ID"
echo "ccloud api-key create --service-account $SERVICE_ACCOUNT_ID --resource $CLUSTER -o json"
OUTPUT=$(ccloud api-key create --service-account $SERVICE_ACCOUNT_ID --resource $CLUSTER -o json)
echo "$OUTPUT" | jq .
API_KEY_SA=$(echo "$OUTPUT" | jq -r ".key")
API_SECRET_SA=$(echo "$OUTPUT" | jq -r ".secret")

echo -e "\n# Create ACLs for the service account"
echo "ccloud kafka acl create --allow --service-account $SERVICE_ACCOUNT_ID --operation CREATE --topic $TOPIC1"
echo "ccloud kafka acl create --allow --service-account $SERVICE_ACCOUNT_ID --operation WRITE --topic $TOPIC1"
ccloud kafka acl create --allow --service-account $SERVICE_ACCOUNT_ID --operation CREATE --topic $TOPIC1
ccloud kafka acl create --allow --service-account $SERVICE_ACCOUNT_ID --operation WRITE --topic $TOPIC1
echo
echo "ccloud kafka acl list --service-account $SERVICE_ACCOUNT_ID"
ccloud kafka acl list --service-account $SERVICE_ACCOUNT_ID
sleep 2



##################################################
# Create ksqlDB Application
##################################################

read -p "Do you acknowledge this script creates a Confluent Cloud KSQL app (hourly charges may apply)? [y/n] " -n 1 -r
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
  printf "\n"

echo -e "\n# Create a new API key for user"
echo "ccloud api-key create --description \"Demo credentials\" --resource \"cluster_id\" -o json"
OUTPUT_KSQL_CREATE_KEY=$(ccloud api-key create --description "Demo credentials" --resource $CLUSTER -o json)
status=$?
if [[ $status != 0 ]]; then
  echo "ERROR: Failed to create an API key.  Please troubleshoot and run again"
  exit 1
fi
echo "$OUTPUT_KSQL_CREATE_KEY" | jq .

API_KEY_CREATE_KSQL=$(echo "$OUTPUT_KSQL_CREATE_KEY" | jq -r ".key")
API_SECRET_CREATE_KSQL=$(echo "$OUTPUT_KSQL_CREATE_KEY" | jq -r ".secret")
echo -e "\n# Associate the API key $API_KEY_CREATE_KSQL to the Kafka cluster $CLUSTER"
echo "ccloud api-key store $API_KEY_CREATE_KSQL $API_SECRET_CREATE_KSQL --resource $CLUSTER"

echo "ccloud api-key use $API_KEY_CREATE_KSQL --resource $CLUSTER"
ccloud api-key use $API_KEY_CREATE_KSQL --resource $CLUSTER


ccloud ksql app create odds-calculation-app --api-key $API_KEY_CREATE_KSQL --api-secret $API_SECRET_CREATE_KSQL
#! ccloud ksql app configure-acls

echo "Provisioning ksqlDB application"

ksqldb_meta=$(ccloud ksql app list -o json | jq -r 'map(select(.endpoint == "'"$ksqldb_endpoint"'")) | .[]')
ksqldb_status=$(ccloud ksql app list -o json | jq '.[].status')
ksqldb_status=$(echo $ksqldb_status | tr -d '"')
 
while [ $ksqldb_status != "UP" ]; do
    echo "Waiting 60 seconds for ksqlDB to come up"
    sleep 60
    ksqldb_status=$(ccloud ksql app list -o json | jq '.[].status')
    ksqldb_status=$(echo $ksqldb_status | tr -d '"')
  done

echo "ksql is up moving on" 
#NEED TO ADD IN A FAILSAFE TO MAKE SURE IT'S UP 


##################################################
# Run ksql queries 
##################################################


#NEED TO CHECK THE PULL OF THE ENDPOINT AND THE API KEYS

echo "pull endpoint"

ENDPOINT=$(ccloud ksql app list -o json | jq '.[].endpoint')

echo "endpoint for ksqlDB cluster is $ENDPOINT"

echo "pull ksql cluster id"

OUTPUT_KSQL_CLUSTER_ID=$(ccloud ksql app list -o json | jq '.[].id')
KSQL_CLUSTER_ID=$(echo $OUTPUT_KSQL_CLUSTER_ID | tr -d '"')

echo "cluster id for ksqlDB cluster is $KSQL_CLUSTER_ID"

echo -e "\n# Create a new API key for ksql cluster "
echo "ccloud api-key create --description \"Demo credentials\" --resource \"ksql_cluster_id\" -o json"
OUTPUT_KSQL_KEY=$(ccloud api-key create --description "Demo credentials" --resource $KSQL_CLUSTER_ID -o json)
status=$?
if [[ $status != 0 ]]; then
  echo "ERROR: Failed to create an API key.  Please troubleshoot and run again"
  exit 1
fi
echo "$OUTPUT_KSQL_KEY" | jq .

API_KEY_KSQL=$(echo "$OUTPUT_KSQL_KEY" | jq -r ".key")
API_SECRET_KSQL=$(echo "$OUTPUT_KSQL_KEY" | jq -r ".secret")

URL=$(echo $ENDPOINT | tr -d '"')
URL+="/ksql"

echo "sleeping to allow api-key to be created"

sleep 200

echo "create odds landing stream from odds landing topic" 

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE STREAM oddslanding (driverId BIGINT, raceId BIGINT, oddsPolarity BIGINT, odds BIGINT) WITH (KAFKA_TOPIC = \'odds-landing\', VALUE_FORMAT = \'JSON\');",
  "streamsProperties": {}
}'

echo "create bet landing stream from bet landing topic"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE STREAM betlanding (betId BIGINT KEY, userId BIGINT, driverId BIGINT, betAmt BIGINT) WITH (KAFKA_TOPIC = \'bet-landing\',VALUE_FORMAT = \'JSON\');",
  "streamsProperties": {}
}'


echo "rekey bet landing topic"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL}\
     -d $'{
  "ksql": "CREATE STREAM BETLANDINGREKEYED WITH (KAFKA_TOPIC=\'betlandingrekeyed\', PARTITIONS=6, REPLICAS=3) AS SELECT * FROM BETLANDING F PARTITION BY F.DRIVERID EMIT CHANGES;",
  "streamsProperties": {}
}'


echo "racecardemo stream created from racecardemo topic"

ccloud kafka topic create racecarDemo

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE STREAM racecardemo (raceId BIGINT, driverId BIGINT, lap BIGINT, position BIGINT, time VARCHAR, miliseconds BIGINT, driverRef VARCHAR, forename VARCHAR, surname VARCHAR, dob VARCHAR, nationality VARCHAR, url VARCHAR) WITH (KAFKA_TOPIC = \'racecarDemo\', VALUE_FORMAT = \'JSON\');",
  "streamsProperties": {}
}'


echo "bets stream created from bets topic"

ccloud kafka topic create bets

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE STREAM bets (betId BIGINT KEY, userId BIGINT, raceId BIGINT, driverId BIGINT, betAmt BIGINT, oddsPolarity BIGINT, odds BIGINT) WITH (KAFKA_TOPIC = \'bets\', VALUE_FORMAT = \'JSON\');",
  "streamsProperties": {}
}'

echo "racecarstatus table created"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE TABLE RACECARSTATUS WITH (KAFKA_TOPIC=\'racecarstatus\', PARTITIONS=1, REPLICAS=3) AS SELECT RACECARDEMO.DRIVERID DRIVERID, LATEST_BY_OFFSET(RACECARDEMO.POSITION) CURRENTPOSITION FROM RACECARDEMO RACECARDEMO GROUP BY RACECARDEMO.DRIVERID EMIT CHANGES;",
  "streamsProperties": {}
}'

echo "probabilities stream created from odds landing stream for positive polarity "

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE STREAM PROBABILITIES WITH (KAFKA_TOPIC=\'probabilities\', PARTITIONS=6, REPLICAS=3) AS SELECT ODDSLANDING.DRIVERID DRIVERID, (100 / (CAST(ODDSLANDING.ODDS AS DOUBLE) + 100)) PROBABILITY FROM ODDSLANDING ODDSLANDING WHERE (ODDSLANDING.ODDSPOLARITY = 1) EMIT CHANGES;",
  "streamsProperties": {}
}'


echo "insert into probabilities stream for negative polarity "

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "INSERT INTO probabilities SELECT driverId, ((CAST(ODDS AS DOUBLE)*-1)/((CAST(ODDS AS DOUBLE)*-1)-100)) as probability FROM  ODDSLANDING WHERE oddspolarity = 0 EMIT CHANGES;",
  "streamsProperties": {}
}'


echo "insert into probabilities stream from racecardemo"


curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "INSERT INTO probabilities SELECT driverId, (20-(cast(position as DOUBLE)))/19 as probability FROM racecardemo EMIT CHANGES;",
  "streamsProperties": {}
}'


echo "create probabilities average table"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE TABLE PROBABILITIESAVERAGE WITH (KAFKA_TOPIC=\'probabilitiesaverage\', PARTITIONS=6, REPLICAS=3) AS SELECT PROBABILITIES.DRIVERID DRIVERID, 1 RACEID, AVG(PROBABILITIES.PROBABILITY) PROBABILITY FROM PROBABILITIES PROBABILITIES GROUP BY PROBABILITIES.DRIVERID EMIT CHANGES;",
  "streamsProperties": {}
}'

echo "create racebetaggregation table"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE TABLE RACEBETAGGREGATION WITH (KAFKA_TOPIC=\'racebetaggregation\', PARTITIONS=6, REPLICAS=3) AS SELECT BETS.RACEID RACEID, COUNT(BETS.BETID) NUMBETS, SUM(BETS.BETAMT) TOTALBETMONEY FROM BETS BETS GROUP BY BETS.RACEID EMIT CHANGES;",
  "streamsProperties": {}
}'

echo "create driverbetaggregation table"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE TABLE DRIVERBETAGGREGATION WITH (KAFKA_TOPIC=\'driverbetaggregation\', PARTITIONS=6, REPLICAS=3) AS SELECT BETS.DRIVERID DRIVERID, MAX(BETS.RACEID) RACEID, COUNT(BETS.BETID) NUMBETS, SUM(BETS.BETAMT) TOTALBETMONEY FROM BETS BETS GROUP BY BETS.DRIVERID EMIT CHANGES;",
  "streamsProperties": {}
}'

echo "create betaggregation table"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE TABLE BETAGGREGATION WITH (KAFKA_TOPIC=\'betaggregation\', PARTITIONS=6, REPLICAS=3) AS SELECT D.DRIVERID DRIVERID, R.RACEID RACEID, (CAST(D.TOTALBETMONEY AS DOUBLE) / CAST(R.TOTALBETMONEY AS DOUBLE)) PCTBETAMT FROM DRIVERBETAGGREGATION D INNER JOIN RACEBETAGGREGATION R ON ((D.RACEID = R.RACEID)) EMIT CHANGES;",
  "streamsProperties": {}
}'

echo "odds table created"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "CREATE TABLE ODDS WITH (KAFKA_TOPIC=\'odds\', PARTITIONS=6, REPLICAS=3) AS SELECT PROBABILITIESAVERAGE.DRIVERID DRIVERID, 1 RACEID, (CASE WHEN (BETAGGREGATION.PCTBETAMT IS NULL) AND (PROBABILITIESAVERAGE.PROBABILITY > .5) THEN CAST (0 AS BIGINT) WHEN (BETAGGREGATION.PCTBETAMT IS NULL) AND (PROBABILITIESAVERAGE.PROBABILITY <= .5) THEN CAST (1 AS BIGINT) WHEN (((BETAGGREGATION.PCTBETAMT + PROBABILITIESAVERAGE.PROBABILITY) / 2) > 0.50) THEN CAST(0 AS BIGINT) ELSE CAST(1 AS BIGINT) END) ODDSPOLARITY, (CASE WHEN (BETAGGREGATION.PCTBETAMT IS NULL) AND (PROBABILITIESAVERAGE.PROBABILITY > .5) THEN CAST ((100 / ((1 / PROBABILITIESAVERAGE.PROBABILITY) -1 )) AS BIGINT) WHEN (BETAGGREGATION.PCTBETAMT IS NULL) AND (PROBABILITIESAVERAGE.PROBABILITY <= .5) THEN CAST ((((1 / PROBABILITIESAVERAGE.PROBABILITY) - 1) * 100) AS BIGINT) WHEN (((BETAGGREGATION.PCTBETAMT + PROBABILITIESAVERAGE.PROBABILITY) / 2) > 0.50) THEN CAST((100 / ((1 / ((BETAGGREGATION.PCTBETAMT + PROBABILITIESAVERAGE.PROBABILITY) / 2)) - 1)) AS BIGINT) ELSE CAST((((1 / ((BETAGGREGATION.PCTBETAMT + PROBABILITIESAVERAGE.PROBABILITY) / 2)) - 1) * 100) AS BIGINT) END) ODDS FROM PROBABILITIESAVERAGE PROBABILITIESAVERAGE LEFT OUTER JOIN BETAGGREGATION BETAGGREGATION ON ((PROBABILITIESAVERAGE.DRIVERID = BETAGGREGATION.DRIVERID)) EMIT CHANGES;",
  "streamsProperties": {}
}'

echo "insert into bets from landing joined to odds"

curl -X "POST" $URL \
     -H "Accept: application/vnd.ksql.v1+json" \
     -u ${API_KEY_KSQL}:${API_SECRET_KSQL} \
     -d $'{
  "ksql": "INSERT INTO bets SELECT f.betId, f.userId, cast(o.raceId AS BIGINT) as raceId, f.driverId as driverId, f.betAmt, o.oddsPolarity, o.odds FROM betlandingrekeyed f join odds o on f.driverId = o.driverId PARTITION BY f.betId EMIT CHANGES;",
  "streamsProperties": {}
}'

echo
echo "clean up racecarDemo topic -- should be created with race start!"
ccloud kafka topic delete racecarDemo

##################################################
# Run odds-landing producer to produce initial odds to cluster 
##################################################

node node/odds-producer.js $BOOTSTRAP_SERVERS $API_KEY_SA $API_SECRET_SA
