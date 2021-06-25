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

import org.gbif.embl.util.DwcArchiveBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.gbif.embl.util.EmblAdapterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.embl.util.EmblAdapterConstants.DATE_NO_SEPARATORS_FORMAT;

public abstract class ArchiveGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorTask.class);

  private final DwcArchiveBuilder archiveBuilder;
  private final TaskConfiguration taskConfiguration;
  private final String workingDirectory;

  public ArchiveGeneratorTask(
      TaskConfiguration taskConfiguration,
      String workingDirectory,
      DwcArchiveBuilder archiveBuilder) {
    this.taskConfiguration = taskConfiguration;
    this.workingDirectory = workingDirectory;
    this.archiveBuilder = archiveBuilder;
  }

  @Override
  public void run() {
    LOG.info("[{}] Start running task", taskConfiguration.name);

    // download non-CON sequences
    CommandLine downloadSequencesCommand = new CommandLine("curl");
    downloadSequencesCommand.addArgument(taskConfiguration.requestUrl1);
    downloadSequencesCommand.addArgument("-o");
    downloadSequencesCommand.addArgument(taskConfiguration.rawDataFile1);

    // download wgs_set
    CommandLine downloadWgsSetCommand = new CommandLine("curl");
    downloadWgsSetCommand.addArgument(taskConfiguration.requestUrl2);
    downloadWgsSetCommand.addArgument("-o");
    downloadWgsSetCommand.addArgument(taskConfiguration.rawDataFile2);

    DefaultExecutor executor = new DefaultExecutor();
    executor.setExitValue(0);

    try {
      // download data
      LOG.info("[{}] Start downloading data", taskConfiguration.name);
      executor.execute(downloadSequencesCommand);
      executor.execute(downloadWgsSetCommand);
      String tableName = prepareRawData();

      // create archive
      LOG.info("[{}] Start creating archive", taskConfiguration.name);
      String archiveName =
          String.format(
              taskConfiguration.archiveName, LocalDate.now().format(DATE_NO_SEPARATORS_FORMAT));
      archiveBuilder.buildArchive(
          new File(workingDirectory + "/output", archiveName),
          tableName,
          taskConfiguration.query,
          taskConfiguration.metadataFile,
          "datasetWithHosts".equals(taskConfiguration.name) ? EmblAdapterConstants.TERMS_WITH_ASSOCIATED_TAXA : EmblAdapterConstants.TERMS);
      LOG.info("[{}] Archive {} was created", taskConfiguration.name, archiveName);

      // delete temp files
      Files.deleteIfExists(Paths.get(taskConfiguration.rawDataFile1));
      Files.deleteIfExists(Paths.get(taskConfiguration.rawDataFile2));
      LOG.info(
          "[{}] Raw data file {} deleted", taskConfiguration.name, taskConfiguration.rawDataFile1);
      LOG.info(
          "[{}] Raw data file {} deleted", taskConfiguration.name, taskConfiguration.rawDataFile2);
    } catch (IOException e) {
      LOG.error("[{}] IOException while producing archive", taskConfiguration.name, e);
    } catch (SQLException e) {
      LOG.error("[{}] SQLException while producing archive", taskConfiguration.name, e);
    }
  }

  protected abstract String prepareRawData() throws IOException, SQLException;
}
