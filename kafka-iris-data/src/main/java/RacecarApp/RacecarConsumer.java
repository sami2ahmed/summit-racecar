package RacecarApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class RacecarConsumer {

    private final Logger LOG = LoggerFactory.getLogger(RacecarConsumer.class);

    @KafkaListener(topics="racecar", groupId = "racecar-consumer")
    private void processMessage(String message) {
        LOG.info(message);
    }

}
