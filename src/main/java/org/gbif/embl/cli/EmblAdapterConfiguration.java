/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.embl.cli;

import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;

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
  @Parameter(names = "--dataset-for-edna-metadata-file")
  public String datasetForEdnaMetadataFile;

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
  @Parameter(names = "--dataset-for-organism-sequenced-metadata-file")
  public String datasetForOrganismSequencedMetadataFile;

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
  @Parameter(names = "--dataset-with-hosts-metadata-file")
  public String datasetWithHostsMetadataFile;

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
        .add("datasetForEdnaRawDataFile=" + datasetForEdnaRawDataFile)
        .add("datasetForOrganismSequencedRawDataFile=" + datasetForOrganismSequencedRawDataFile)
        .add("datasetWithHostsRawDataFile=" + datasetWithHostsRawDataFile)
        .add("datasetForEdnaArchiveName=" + datasetForEdnaArchiveName)
        .add("datasetForOrganismSequencedArchiveName=" + datasetForOrganismSequencedArchiveName)
        .add("datasetWithHostsArchiveName=" + datasetWithHostsArchiveName)
        .add("datasetForEdnaRequestUrl=" + datasetForEdnaRequestUrl)
        .add("datasetForOrganismSequencedRequestUrl=" + datasetForOrganismSequencedRequestUrl)
        .add("datasetWithHostsRequestUrl=" + datasetWithHostsRequestUrl)
        .add("datasetForEdnaMetadataFile=" + datasetForEdnaMetadataFile)
        .add("datasetForOrganismSequencedMetadataFile=" + datasetForOrganismSequencedMetadataFile)
        .add("datasetWithHostsMetadataFile=" + datasetWithHostsMetadataFile)
        .toString();
  }
}
