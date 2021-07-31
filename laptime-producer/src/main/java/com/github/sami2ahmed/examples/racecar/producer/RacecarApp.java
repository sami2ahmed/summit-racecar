package com.github.sami2ahmed.examples.racecar.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableKafka
public class RacecarApp {

    public static void main(String[] args) {
        SpringApplication.run(RacecarApp.class, args);
    }

}
