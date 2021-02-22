package org.gbif.embl.cli;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

@SuppressWarnings("PublicField")
public class Task {

  @NotNull
  public String name;

  @NotNull
  public String metadataFile;

  @NotNull
  public String requestUrl;

  @NotNull
  public String rawDataFile;

  @NotNull
  public String archiveName;

  @Override
  public String toString() {
    return new StringJoiner(", ", Task.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("metadataFile='" + metadataFile + "'")
        .add("requestUrl='" + requestUrl + "'")
        .add("rawDataFile='" + rawDataFile + "'")
        .add("archiveName='" + archiveName + "'")
        .toString();
  }
}
