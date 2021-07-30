package io.woolford.RacecarApp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


@Component
public class RacecarProducer {

    private final Logger logger = LoggerFactory.getLogger(RacecarProducer.class);

    @Autowired
    KafkaTemplate kafkaTemplate;

    private List<RacecarRecord> racecarRecordList;

    public RacecarProducer() throws IOException {

        String driver_laps_sorted_cleanedCsvString = "";
        ClassPathResource cpr = new ClassPathResource("driver_laps_sorted_final.csv");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            driver_laps_sorted_cleanedCsvString = new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("IOException", e);
        }

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

        CsvMapper csvMapper = new CsvMapper();
        MappingIterator<RacecarRecord> racecarIter = csvMapper.readerFor(RacecarRecord.class).with(schema).readValues(driver_laps_sorted_cleanedCsvString);
        this.racecarRecordList = racecarIter.readAll();

    }

    @Bean
    public NewTopic createRacecarTopic() {
        Map<String, String> props = new HashMap<>();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(10000));

        return TopicBuilder.name("racecarDemo")
                .partitions(1)
                .replicas(3).configs(props)
                .build();
    }


   // @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    private void publishRacecarRecord() throws JsonProcessingException {

        ListIterator<RacecarRecord> itr = racecarRecordList.listIterator();
        while (itr.hasNext()) {
            RacecarRecord racecarRecord = itr.next();

// verify why we cannot put json onto the topic

        ObjectMapper mapper = new ObjectMapper();
        String racecarRecordJson = mapper.writeValueAsString(racecarRecord);
        JsonNode finalJsonString = mapper.readTree(racecarRecordJson);
        kafkaTemplate.send("racecarDemo", racecarRecordJson);

        }

    }

}