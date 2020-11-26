package org.gbif.embl.cli;

import com.beust.jcommander.Parameter;

import java.util.StringJoiner;

public class ClientConfiguration {

  @Parameter(names = "--timeout")
  public int timeout = 10000;

  @Parameter(names = "--max-connections")
  public int maxConnections = 100;

  @Parameter(names = "--max-per-route")
  public int maxPerRoute = 100;

  @Parameter(names = "--max-attempts")
  public int maxAttempts = 3;

  @Parameter(names = "--initial-interval")
  public long initialInterval = 5000L;

  @Parameter(names = "--multiplier")
  public double multiplier = 1.5;

  @Override
  public String toString() {
    return new StringJoiner(", ", ClientConfiguration.class.getSimpleName() + "[", "]")
        .add("timeout=" + timeout)
        .add("maxConnections=" + maxConnections)
        .add("maxPerRoute=" + maxPerRoute)
        .add("maxAttempts=" + maxAttempts)
        .add("initialInterval=" + initialInterval)
        .add("multiplier=" + multiplier)
        .toString();
  }
}
