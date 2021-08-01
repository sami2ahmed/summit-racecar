package com.example.racecarevents.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sami2ahmed.examples.racecar.model.LapTime;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
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

    private static final String RACE_CAR_TOPIC = "racecarDemo";

    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS = "pkc-ep9mm.us-east-2.aws.confluent.cloud:9092";

    @Value("${spring.kafka.security.protocol}")
    private String SECURITY_PROTOCOL = "SASL_SSL";

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism = "PLAIN";

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule   required username='LCQIWCT2UPTJTUEG'   password='UVW3BXLJWnsbL9tVnYU3Y1Y0uP4i4EIHypjYgE70ceej62KsKbzYzWcpT9HccB0O';";

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LapTime> getOrdersEventsFlux(@RequestParam(name = "raceId") String driverId){
        Map<String, Object> propsMaps = this.kafkaReceiverConfigurations(driverId);


        Deserializer<LapTime> deserializer = new JsonDeserializer<LapTime>();
        deserializer.configure(propsMaps, false);
        ReceiverOptions<String, LapTime> receiverOptions = ReceiverOptions.<String, LapTime>create(propsMaps)
            .withValueDeserializer(deserializer)
            .withKeyDeserializer(new StringDeserializer())
            .subscription(Collections.singletonList(RACE_CAR_TOPIC));

        DefaultKafkaReceiver<String, LapTime> kafkaReceiver =
                new DefaultKafkaReceiver<String,LapTime>(ConsumerFactory.INSTANCE, receiverOptions);

        Flux<ReceiverRecord<String, LapTime>> kafkaFlux = kafkaReceiver.receive();

        return kafkaFlux
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
        //props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, JsonNode.class);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL);
        props.put("sasl.mechanism",saslMechanism);
        props.put("sasl.jaas.config",jaasConfig);
        return props;
    }
}

