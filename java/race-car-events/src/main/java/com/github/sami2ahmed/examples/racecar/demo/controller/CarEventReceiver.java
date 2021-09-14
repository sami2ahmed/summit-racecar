package com.example.racecarevents.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sami2ahmed.examples.racecar.model.LapTime;
import com.github.sami2ahmed.examples.racecar.model.Odds;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(CarEventReceiver.class);
    private static final String RACE_CAR_TOPIC = "racecarDemo";
    private static final String ODDS_LANDING = "odds-landing";

    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS = "";

    @Value("${spring.kafka.security.protocol}")
    private String SECURITY_PROTOCOL = "";

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism = "";

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String jaasConfig = "";


    @GetMapping(value = "/odds/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Odds> getOdds(){
        log.info("getOdds()");
        Map<String, Object> propsMaps = this.kafkaReceiverConfigurations("odds");


        Deserializer<Odds> deserializer = new JsonDeserializer<Odds>();
        deserializer.configure(propsMaps, false);
        ReceiverOptions<String, Odds> receiverOptions = ReceiverOptions.<String, Odds>create(propsMaps)
                .withValueDeserializer(deserializer)
                .withKeyDeserializer(new StringDeserializer())
                .subscription(Collections.singletonList(ODDS_LANDING));

        DefaultKafkaReceiver<String, Odds> kafkaReceiver =
                new DefaultKafkaReceiver<>(ConsumerFactory.INSTANCE, receiverOptions);

        Flux<ReceiverRecord<String, Odds>> kafkaFlux = kafkaReceiver.receive();

        return kafkaFlux
                .map(ReceiverRecord::value).log();

    }


    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<LapTime> getOrdersEventsFlux(@RequestParam(name = "raceId") String driverId){
        log.info("getOrdersEventsFlux() driverId = '{}'", driverId);
        Map<String, Object> propsMaps = this.kafkaReceiverConfigurations(driverId);


        Deserializer<LapTime> deserializer = new JsonDeserializer<LapTime>();
        deserializer.configure(propsMaps, false);
        ReceiverOptions<String, LapTime> receiverOptions = ReceiverOptions.<String, LapTime>create(propsMaps)
                .withValueDeserializer(deserializer)
                .withKeyDeserializer(new StringDeserializer())
                .subscription(Collections.singletonList(RACE_CAR_TOPIC));

        DefaultKafkaReceiver<String, LapTime> kafkaReceiver =
                new DefaultKafkaReceiver<>(ConsumerFactory.INSTANCE, receiverOptions);

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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, id);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, JsonNode.class);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL);
        props.put("sasl.mechanism",saslMechanism);
        props.put("sasl.jaas.config",jaasConfig);
        return props;
    }
}

