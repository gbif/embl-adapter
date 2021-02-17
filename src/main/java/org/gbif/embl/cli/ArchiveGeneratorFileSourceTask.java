package org.gbif.embl.cli;

import org.gbif.embl.util.DwcArchiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveGeneratorFileSourceTask extends ArchiveGeneratorTask {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorDatabaseSourceTask.class);

  public ArchiveGeneratorFileSourceTask(
      String taskName,
      String requestUrl,
      String archiveNameTemplate,
      String rawDataFile,
      String workingDirectory,
      String metadataFilePath,
      DwcArchiveBuilder archiveBuilder) {
    super(
        taskName,
        requestUrl,
        archiveNameTemplate,
        rawDataFile,
        workingDirectory,
        metadataFilePath,
        archiveBuilder);
  }

  protected String prepareRawData() {
    LOG.debug("File raw data");
    return getRawDataFileName();
  }
}
