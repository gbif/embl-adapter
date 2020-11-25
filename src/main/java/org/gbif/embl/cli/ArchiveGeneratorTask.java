package org.gbif.embl.cli;

import org.gbif.embl.util.DbConnectionUtils;
import org.gbif.embl.util.DwcArchiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import static org.gbif.embl.util.EmblAdapterConstants.SELECT;

public class ArchiveGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorTask.class);

  private static final String ARCHIVE_NAME_TEMPLATE = "embl-archive_%s.zip";

  @Override
  public void run() {
    LOG.info("Start creating archive");
    DwcArchiveBuilder dwcArchiveBuilder = new DwcArchiveBuilder();

    try (Connection connection = DbConnectionUtils.dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(SELECT)) {
      String archiveName = String.format(ARCHIVE_NAME_TEMPLATE, new Date().getTime());
      dwcArchiveBuilder.buildArchive(
          new File("output", archiveName), resultSet);
      LOG.info("Archive {} was created", archiveName);
    } catch (SQLException e) {
      LOG.error("SQL exception while running archive building task");
    }
  }
}
