package org.gbif.embl.cli;

import com.beust.jcommander.Parameter;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

public class ClientConfiguration {

  @NotNull
  @Parameter(names = "--max-attempts")
  public int maxAttempts = 3;

  @NotNull
  @Parameter(names = "--initial-interval")
  public long initialInterval = 5000L;

  @NotNull
  @Parameter(names = "--multiplier")
  public double multiplier = 1.5;

  @Override
  public String toString() {
    return new StringJoiner(", ", ClientConfiguration.class.getSimpleName() + "[", "]")
        .add("maxAttempts=" + maxAttempts)
        .add("initialInterval=" + initialInterval)
        .add("multiplier=" + multiplier)
        .toString();
  }
}
