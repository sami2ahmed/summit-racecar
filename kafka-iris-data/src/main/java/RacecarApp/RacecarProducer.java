package RacecarApp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.stream.IntStream;


@Component
public class RacecarProducer {

    private final Logger logger = LoggerFactory.getLogger(RacecarProducer.class);

    @Autowired
    KafkaTemplate kafkaTemplate;

    private List<RacecarRecord> racecarRecordList;

    public RacecarProducer() throws IOException {

        String driver_laps_sorted_cleanedCsvString = "";
        ClassPathResource cpr = new ClassPathResource("driver_laps_sorted_cleaned.csv");
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
        return TopicBuilder.name("racecar")
                .partitions(1)
                .replicas(3)
                .build();
    }


    @Scheduled(fixedDelay = 100L)
    private void publishRacecarRecord() throws JsonProcessingException {

        ListIterator<RacecarRecord> itr = racecarRecordList.listIterator();
        while (itr.hasNext()) {
            RacecarRecord racecarRecord = itr.next();

        ObjectMapper mapper = new ObjectMapper();
        String racecarRecordJson = mapper.writeValueAsString(racecarRecord);
        kafkaTemplate.send("racecar", racecarRecordJson);

        }

    }

}