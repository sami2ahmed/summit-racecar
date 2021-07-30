package com.github.sami2ahmed.examples.racecar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableLapTime.class)
public interface LapTime {
  @JsonProperty("raceId")
  Integer raceId();

  @JsonProperty("driverId")
  String driverId();

  @JsonProperty("lap")
  Integer lap();

  @JsonProperty("position")
  Integer position();

  @JsonProperty("time")
  String time();

  @JsonProperty("milliseconds")
  Integer milliseconds();

  @JsonProperty("driverRef")
  String driverRef();

  @JsonProperty("forename")
  String forename();

  @JsonProperty("surname")
  String surname();

  @JsonProperty("dob")
  String dob();

  @JsonProperty("nationality")
  String nationality();

  @JsonProperty("url")
  String url();
}
