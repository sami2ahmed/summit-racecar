package com.github.sami2ahmed.examples.racecar.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as=ImmutableRaceDetails.class)
public interface RaceDetails {
  String raceId();

  boolean finished();
}
