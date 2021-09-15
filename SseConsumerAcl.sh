#!/bin/bash

CLUSTER_ID=$(ccloud kafka cluster list -o json | jq -r 'map(select(.name == "demo-kafka-cluster")) | .[].id')
echo -e "\n# selecting existing basic cluster spun up earlier"
ccloud kafka cluster use $CLUSTER_ID

echo -e "\n# Create a new service account"
RANDOM_NUM=$((1 + RANDOM % 1000000))
SERVICE_NAME="demo-app-$RANDOM_NUM"
echo "ccloud service-account create $SERVICE_NAME --description $SERVICE_NAME -o json"
OUTPUT=$(ccloud service-account create $SERVICE_NAME --description $SERVICE_NAME  -o json)
echo "$OUTPUT" | jq .
SERVICE_ACCOUNT_ID=$(echo "$OUTPUT" | jq -r ".id")

echo -e "\n# Create an API key and secret for the service account $SERVICE_ACCOUNT_ID"
echo "ccloud api-key create --service-account $SERVICE_ACCOUNT_ID --resource $CLUSTER_ID -o json"
OUTPUT=$(ccloud api-key create --service-account $SERVICE_ACCOUNT_ID --resource $CLUSTER_ID -o json)
echo "$OUTPUT" | jq .
API_KEY_SA=$(echo "$OUTPUT" | jq -r ".key")
API_SECRET_SA=$(echo "$OUTPUT" | jq -r ".secret")

echo -e "\n# Create ACLs for the service account"

echo "Consumer group access for input and internal topics by application.id prefix"
ccloud kafka acl create --allow --service-account $SERVICE_ACCOUNT_ID --operation READ --operation delete --operation describe --consumer-group car-consumer --prefix

echo "Give service account access to all topics in cluster"
ccloud kafka acl create --allow --service-account $SERVICE_ACCOUNT_ID --operation READ --operation delete --operation describe --topic '*'

echo
echo "ccloud kafka acl list --service-account $SERVICE_ACCOUNT_ID"
ccloud kafka acl list --service-account $SERVICE_ACCOUNT_ID
sleep 2
