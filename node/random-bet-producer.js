/*
creates an even distribution of bets by randomly betting on drivers 
should be run like so: 
node bet-producer.js <bootstrap-server> <api-key> <api-secret> <# of bets to saturate> 
ie: node bet-producer.js <bootstrap-server> <api-key> <api-secret>  400
*/
var myArgs = process.argv.slice(2);
const { Kafka } = require('kafkajs')
const jsf = require('json-schema-faker');
jsf.extend('faker', () => require('faker'));
const conf = {
    clientId: 'bet-producer',
    brokers: [myArgs[0]],
    ssl: true,
    sasl: {
      mechanism: 'plain',
      username: myArgs[1],
      password: myArgs[2],
    },
  }


const kafka = new Kafka(conf)
const producer = kafka.producer()

jsf.option({useExamplesValue: true})

var sendMessages = async() => {
  await producer.connect()
  for (let i=0; i < myArgs[3]; i++){
    const bet = {
      "type": "object",
      "required": ["betId", "userId", "driverId", "betAmt"],
      properties: {
        betId: {
          type: 'integer',
          unique: true,
          minimum: 0,
          exclusiveMinimum: true
        },
        userId: {
          type: "integer",
          minimum: 1,
          maximum: 100000
        },
        driverId: {
          type: "integer",
          examples: [1,2,3,4,6,7,8,9,10,12,13,15,16,17,18,20,21,22,67]
        },
        betAmt: {
          type: "integer",
          minimum: 1,
          maximum: 100
        }
      }
    }
    console.log(jsf.generate(bet)["betId"])
    await producer.send({
      topic: 'bet-landing',
      messages: [
        {key: (jsf.generate(bet)["betId"]).toString(), value: JSON.stringify(jsf.generate(bet))}
      ]
    })
  }
  await producer.disconnect()
}

sendMessages();
