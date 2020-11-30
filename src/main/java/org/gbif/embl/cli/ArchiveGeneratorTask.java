package org.gbif.embl.cli;

import org.gbif.embl.util.DwcArchiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.gbif.embl.util.EmblAdapterConstants.SELECT;

public class ArchiveGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorTask.class);

  private static final String ARCHIVE_NAME_TEMPLATE = "embl-archive_%s.zip";

  private final DataSource dataSource;
  private final CyclicBarrier barrier;
  private final String workingDirectory;

  public ArchiveGeneratorTask(
      DataSource dataSource,
      CyclicBarrier barrier,
      String workingDirectory) {
    this.dataSource = dataSource;
    this.barrier = barrier;
    this.workingDirectory = workingDirectory;
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
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(SELECT)) {
      DwcArchiveBuilder dwcArchiveBuilder = new DwcArchiveBuilder(workingDirectory);

      String archiveName = String.format(ARCHIVE_NAME_TEMPLATE, new Date().getTime());
      dwcArchiveBuilder.buildArchive(
          new File(workingDirectory + "/output", archiveName), resultSet);
      LOG.info("Archive {} was created", archiveName);
    } catch (SQLException e) {
      LOG.error("SQL exception while running archive building task", e);
    }
  }
}
