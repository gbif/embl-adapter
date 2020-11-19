package org.gbif.embl.cli;

import com.beust.jcommander.Parameter;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

public class EmblAdapterConfiguration {

  @NotNull
  @Parameter(names = "--start-time")
  public String startTime;

  @NotNull
  @Parameter(names = "--frequency-in-days")
  public Integer frequencyInDays = 7;

  @Override
  public String toString() {
    return new StringJoiner(", ", EmblAdapterConfiguration.class.getSimpleName() + "[", "]")
        .add("startTime='" + startTime + "'")
        .add("frequencyInDays=" + frequencyInDays)
        .toString();
  }
}
