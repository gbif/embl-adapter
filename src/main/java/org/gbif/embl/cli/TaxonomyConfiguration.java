package org.gbif.embl.cli;

import javax.validation.constraints.NotNull;
import java.util.StringJoiner;

public class TaxonomyConfiguration {

  public boolean skipUpdate = false;

  @NotNull
  public String archiveUrl;

  @NotNull
  public String archiveName;

  @NotNull
  public String tempDir;

  @NotNull
  public String extractedDir;

  @Override
  public String toString() {
    return new StringJoiner(", ", TaxonomyConfiguration.class.getSimpleName() + "[", "]")
        .add("skipUpdate='" + skipUpdate + "'")
        .add("archiveUrl='" + archiveUrl + "'")
        .add("archiveName='" + archiveName + "'")
        .add("tempDir='" + tempDir + "'")
        .add("extractedDir='" + extractedDir + "'")
        .toString();
  }
}
