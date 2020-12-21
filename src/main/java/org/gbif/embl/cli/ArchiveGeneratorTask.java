package org.gbif.embl.cli;

import org.gbif.embl.util.DwcArchiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ArchiveGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorTask.class);

  private static final String ARCHIVE_NAME_TEMPLATE = "embl-archive_%s.zip";

  private final String rawDataFile;
  private final CyclicBarrier barrier;
  private final String workingDirectory;
  private final String metadataFilePath;

  public ArchiveGeneratorTask(
      String rawDataFile,
      CyclicBarrier barrier,
      String workingDirectory,
      String metadataFilePath) {
    this.rawDataFile = rawDataFile;
    this.barrier = barrier;
    this.workingDirectory = workingDirectory;
    this.metadataFilePath = metadataFilePath;
  }

  @Override
  public void run() {
    LOG.info("Task started, waiting for the others to finish");
    try {
      barrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      LOG.error("Exception while waiting other tasks", e);
    }

    LOG.info("Start creating archive");
    DwcArchiveBuilder dwcArchiveBuilder = new DwcArchiveBuilder(workingDirectory, metadataFilePath);
    String archiveName = String.format(ARCHIVE_NAME_TEMPLATE, new Date().getTime());
    dwcArchiveBuilder.buildArchive(
        new File(workingDirectory + "/output", archiveName), rawDataFile);
    LOG.info("Archive {} was created", archiveName);

    try {
      Files.deleteIfExists(Paths.get(rawDataFile));
      LOG.info("Raw data file {} deleted", rawDataFile);
    } catch (IOException e) {
      LOG.error("IOException while trying to delete raw data file");
    }
  }
}
