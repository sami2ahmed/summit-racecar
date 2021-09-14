/*
should be run as so: 
node odds-producer.js <bootstrap-server> <api-key> <api-secret>
*/
var myArgs = process.argv.slice(2);

const { Kafka } = require('kafkajs')
const jsf = require('json-schema-faker');
jsf.extend('faker', () => require('faker'));
const conf = {
    clientId: 'odds-producer',
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
const cars = [1,2,3,4,6,7,8,9,10,12,13,15,16,17,18,20,21,22,67]
jsf.option({useExamplesValue: true})

var sendMessages = async() => {
  await producer.connect()
  for (i in cars){
    const odds = {
      "type": "object",
      "required": ["driverId", "raceId", "oddsPolarity", "odds"],
      properties: {
        driverId: {
          type: 'integer',
          unique: true,
          examples: [cars[i]]
        },
        raceId: {
          type: "integer",
          examples: [1]
        },
        oddsPolarity: {
          type: "integer",
          examples: [0,1]
        },
        odds: {
          type: "integer",
          examples: [150, 200, 250, 300, 350, 400, 450, 500, 600]
        }
      }
    }
    console.log(jsf.generate(odds)["driverId"])
    await producer.send({
      topic: 'odds-landing',
      messages: [
        {key: (jsf.generate(odds)["driverId"]).toString(), value: JSON.stringify(jsf.generate(odds))}
      ]
    })
  }
  await producer.disconnect()
}

sendMessages();
