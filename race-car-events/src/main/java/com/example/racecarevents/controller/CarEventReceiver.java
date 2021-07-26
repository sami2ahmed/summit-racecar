package com.example.racecarevents.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.receiver.internals.ConsumerFactory;
import reactor.kafka.receiver.internals.DefaultKafkaReceiver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/api")
public class CarEventReceiver {

    private static final String RACE_CAR_TOPIC = "racecar";

    @Value("${spring.kafka.bootstrap-servers}")
    private static String BOOTSTRAP_SERVERS = "pkc-ep9mm.us-east-2.aws.confluent.cloud:9092";

    @Value("${spring.kafka.security.protocol}")
    private static String SECURITY_PROTOCOL = "SASL_SSL";

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private static String saslMechanism = "PLAIN";

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private static String jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule   required username='LCQIWCT2UPTJTUEG'   password='UVW3BXLJWnsbL9tVnYU3Y1Y0uP4i4EIHypjYgE70ceej62KsKbzYzWcpT9HccB0O';";

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<JsonNode> getOrdersEventsFlux(@RequestParam(name = "driverId") String driverId){
        Map<String, Object> propsMaps = this.kafkaReceiverConfigurations(driverId);

        DefaultKafkaReceiver<String, JsonNode> kafkaReceiver =
                new DefaultKafkaReceiver(ConsumerFactory.INSTANCE, ReceiverOptions.create(propsMaps).subscription(Collections.singleton(RACE_CAR_TOPIC)));

        Flux<ReceiverRecord<String, JsonNode>> kafkaFlux = kafkaReceiver.receive();

        return kafkaFlux
                .filter(receivedRecord -> {
                    receivedRecord.receiverOffset().acknowledge();
                    return receivedRecord.value().get("driverId").asText().equals(driverId);
                })
                .map(ReceiverRecord::value).log();

    }

    /**
     * Static Kafka Consumer configurations.
     * @param id
     *
     **/
    private Map<String, Object> kafkaReceiverConfigurations(String id){

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "car-consumer-"+id+"-"+ UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, id);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL);
        props.put("sasl.mechanism",saslMechanism);
        props.put("sasl.jaas.config",jaasConfig);
        return props;
    }
}

