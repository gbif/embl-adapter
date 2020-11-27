package org.gbif.embl.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

public class EmblAdapterConfiguration {

  @ParametersDelegate
  @Valid
  @NotNull
  public DbConfiguration db = new DbConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public ClientConfiguration client = new ClientConfiguration();

  @NotNull
  @Parameter(names = "--embl-ebi-api")
  public String emblEbiApi = "https://www.ebi.ac.uk/ena/portal/api/";

  @NotNull
  @Parameter(names = "--start-time")
  public String startTime;

  @NotNull
  @Parameter(names = "--frequency-in-days")
  public Integer frequencyInDays = 7;

  @Parameter(names = "--limit")
  public Integer limit;

  @Override
  public String toString() {
    return new StringJoiner(", ", EmblAdapterConfiguration.class.getSimpleName() + "[", "]")
        .add("db=" + db)
        .add("client=" + client)
        .add("emblEbiApi='" + emblEbiApi + "'")
        .add("startTime='" + startTime + "'")
        .add("frequencyInDays=" + frequencyInDays)
        .add("limit=" + limit)
        .toString();
  }
}
