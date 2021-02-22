package org.gbif.embl.cli;

import java.util.StringJoiner;

@SuppressWarnings("PublicField")
public class TaskConfiguration {

  public String name;

  public String metadataFile;

  public String requestUrl;

  public String rawDataFile;

  public String archiveName;

  @Override
  public String toString() {
    return new StringJoiner(", ", TaskConfiguration.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("metadataFile='" + metadataFile + "'")
        .add("requestUrl='" + requestUrl + "'")
        .add("rawDataFile='" + rawDataFile + "'")
        .add("archiveName='" + archiveName + "'")
        .toString();
  }
}
