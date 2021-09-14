package com.github.sami2ahmed.examples.racecar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as=ImmutableOdds.class)
public interface Odds {
  @JsonProperty("driverId")
  Integer driverId();
  @JsonProperty("raceId")
  Integer raceId();
  @JsonProperty("oddsPolarity")
  Integer oddsPolarity();
  @JsonProperty("odds")
  Integer odds();
}
