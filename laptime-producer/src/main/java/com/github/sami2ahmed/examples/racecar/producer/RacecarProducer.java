package com.github.sami2ahmed.examples.racecar.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.sami2ahmed.examples.racecar.model.LapTime;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Component
public class RacecarProducer {

    private final Logger logger = LoggerFactory.getLogger(RacecarProducer.class);

    @Autowired
    KafkaTemplate<String, LapTime> kafkaTemplate;

    private Iterator<LapTime> records;

    public RacecarProducer() throws IOException {

        CsvSchema schema = CsvSchema.builder()
            .addColumn("raceId", CsvSchema.ColumnType.NUMBER)
            .addColumn("driverId", CsvSchema.ColumnType.NUMBER)
            .addColumn("lap", CsvSchema.ColumnType.NUMBER)
            .addColumn("position", CsvSchema.ColumnType.NUMBER)
            .addColumn("time", CsvSchema.ColumnType.STRING)
            .addColumn("milliseconds", CsvSchema.ColumnType.NUMBER)
            .addColumn("driverRef", CsvSchema.ColumnType.STRING)
            .addColumn("forename", CsvSchema.ColumnType.STRING)
            .addColumn("surname", CsvSchema.ColumnType.STRING)
            .addColumn("dob", CsvSchema.ColumnType.STRING)
            .addColumn("nationality", CsvSchema.ColumnType.STRING)
            .addColumn("url", CsvSchema.ColumnType.STRING)
            .build().withHeader();

        logger.info(".ctor() - Read driver_laps_sorted_final.csv to memory.");
        try(InputStream inputStream = this.getClass().getResourceAsStream("/driver_laps_sorted_final.csv")) {
            CsvMapper csvMapper = new CsvMapper();
            MappingIterator<LapTime> racecarIter = csvMapper.readerFor(LapTime.class)
                .with(schema)
                .readValues(inputStream);
            List<LapTime> records = racecarIter.readAll();
            this.records = records.iterator();
        }
    }

    static final String TOPIC = "racecarDemo";

    @Bean
    public NewTopic createRacecarTopic() {
        Map<String, String> props = new HashMap<>();
        return TopicBuilder.name(TOPIC)
                .partitions(1)
                .replicas(3)
                .configs(props)
                .build();
    }


    @Scheduled(fixedDelay = 10000L)
    private void publishRacecarRecord() throws JsonProcessingException {
        if(!this.records.hasNext()) {
            logger.debug("publishRacecarRecord() - No more records.");
        }

        LapTime record = this.records.next();
        logger.trace("publishRacecarRecord() - sending {}", record);
        ListenableFuture<SendResult<String, LapTime>> future = kafkaTemplate.send(TOPIC, record.driverId(), record);
        future.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable throwable) {
                logger.error("Exception thrown writing to kafka.", throwable);
            }

            @Override
            public void onSuccess(SendResult<String, LapTime> result) {
                if (null != result.getRecordMetadata()) {
                    logger.info("Record written to Kafka. topic = '{}' partition = {} offset = {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                    );
                }
            }
        });
    }

}