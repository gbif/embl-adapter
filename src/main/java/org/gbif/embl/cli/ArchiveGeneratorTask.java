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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.embl.util.EmblAdapterConstants.DATE_NO_SEPARATORS_FORMAT;

public abstract class ArchiveGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorTask.class);

  private final String taskName;
  private final String requestUrl;
  private final String archiveNameTemplate;
  private final String rawDataFile;
  private final String workingDirectory;
  private final String metadataFilePath;
  private final DwcArchiveBuilder archiveBuilder;

  public ArchiveGeneratorTask(
      String taskName,
      String requestUrl,
      String archiveNameTemplate,
      String rawDataFile,
      String workingDirectory,
      String metadataFilePath,
      DwcArchiveBuilder archiveBuilder) {
    this.taskName = taskName;
    this.requestUrl = requestUrl;
    this.archiveNameTemplate = archiveNameTemplate;
    this.rawDataFile = rawDataFile;
    this.workingDirectory = workingDirectory;
    this.metadataFilePath = metadataFilePath;
    this.archiveBuilder = archiveBuilder;
  }

  @Override
  public void run() {
    LOG.info("[{}] Start downloading data", taskName);
    CommandLine cmd = new CommandLine("curl");
    cmd.addArgument("-X");
    cmd.addArgument("GET");
    cmd.addArgument(requestUrl);
    cmd.addArgument("-o");
    cmd.addArgument(rawDataFile);

    DefaultExecutor executor = new DefaultExecutor();
    executor.setExitValue(0);

    try {
      // download data
      executor.execute(cmd);
      String rawDataFileOrTable = prepareRawData();

      // create archive
      LOG.info("[{}] Start creating archive", taskName);
      String archiveName =
          String.format(archiveNameTemplate, LocalDate.now().format(DATE_NO_SEPARATORS_FORMAT));
      archiveBuilder.buildArchive(
          new File(workingDirectory + "/output", archiveName),
          rawDataFileOrTable,
          metadataFilePath);
      LOG.info("[{}] Archive {} was created", taskName, archiveName);

      // delete temp files
      Files.deleteIfExists(Paths.get(rawDataFile));
      LOG.info("[{}] Raw data file {} deleted", taskName, rawDataFile);
    } catch (IOException e) {
      LOG.error("[{}] IOException while producing archive", taskName, e);
    } catch (SQLException e) {
      LOG.error("[{}] SQLException while producing archive", taskName, e);
    }
  }

  public String getRawDataFileName() {
    return rawDataFile;
  }

  protected abstract String prepareRawData() throws IOException, SQLException;
}
