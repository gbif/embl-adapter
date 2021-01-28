package org.gbif.embl.cli;

import com.beust.jcommander.Parameter;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

public class EmblAdapterConfiguration {

  @NotNull
  @Parameter(names = "--embl-ebi-api")
  public String emblEbiApi = "https://www.ebi.ac.uk/ena/portal/api/";

  @NotNull
  @Parameter(names = "--start-time")
  public String startTime;

  @NotNull
  @Parameter(names = "--frequency-in-days")
  public Integer frequencyInDays = 7;

  @NotNull
  @Parameter(names = "--working-directory")
  public String workingDirectory;

  @NotNull
  @Parameter(names = "--metadata-file")
  public String metadataFile;

  @NotNull
  @Parameter(names = "--dataset-for-edna-request-url")
  public String datasetForEdnaRequestUrl;

  @NotNull
  @Parameter(names = "--dataset-for-edna-raw-data-file")
  public String datasetForEdnaRawDataFile;

  @NotNull
  @Parameter(names = "--dataset-for-edna-archive-name")
  public String datasetForEdnaArchiveName;

  @NotNull
  @Parameter(names = "--dataset-for-organism-sequenced-request-url")
  public String datasetForOrganismSequencedRequestUrl;

  @NotNull
  @Parameter(names = "--dataset-for-organism-sequenced-raw-data-file")
  public String datasetForOrganismSequencedRawDataFile;

  @NotNull
  @Parameter(names = "--dataset-for-organism-sequenced-archive-name")
  public String datasetForOrganismSequencedArchiveName;

  @NotNull
  @Parameter(names = "--dataset-with-hosts-request-url")
  public String datasetWithHostsRequestUrl;

  @NotNull
  @Parameter(names = "--dataset-with-hosts-raw-data-file")
  public String datasetWithHostsRawDataFile;

  @NotNull
  @Parameter(names = "--dataset-with-hosts-archive-name")
  public String datasetWithHostsArchiveName;

  @Override
  public String toString() {
    return new StringJoiner(", ", EmblAdapterConfiguration.class.getSimpleName() + "[", "]")
        .add("emblEbiApi='" + emblEbiApi + "'")
        .add("startTime='" + startTime + "'")
        .add("frequencyInDays=" + frequencyInDays)
        .add("workingDirectory=" + workingDirectory)
        .add("metadataFile=" + metadataFile)
        .add("datasetForEdnaRawDataFile=" + datasetForEdnaRawDataFile)
        .add("datasetForOrganismSequencedRawDataFile=" + datasetForOrganismSequencedRawDataFile)
        .add("datasetWithHostsRawDataFile=" + datasetWithHostsRawDataFile)
        .add("datasetForEdnaArchiveName=" + datasetForEdnaArchiveName)
        .add("datasetForOrganismSequencedArchiveName=" + datasetForOrganismSequencedArchiveName)
        .add("datasetWithHostsArchiveName=" + datasetWithHostsArchiveName)
        .add("datasetForEdnaRequestUrl=" + datasetForEdnaRequestUrl)
        .add("datasetForOrganismSequencedRequestUrl=" + datasetForOrganismSequencedRequestUrl)
        .add("datasetWithHostsRequestUrl=" + datasetWithHostsRequestUrl)
        .toString();
  }
}
