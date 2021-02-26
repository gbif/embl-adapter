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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.embl.util.EmblAdapterConstants.DATE_NO_SEPARATORS_FORMAT;

public abstract class ArchiveGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorTask.class);

  private final CyclicBarrier barrier;
  private final DwcArchiveBuilder archiveBuilder;
  private final TaskConfiguration taskConfiguration;
  private final String workingDirectory;

  public ArchiveGeneratorTask(
      CyclicBarrier barrier,
      TaskConfiguration taskConfiguration,
      String workingDirectory,
      DwcArchiveBuilder archiveBuilder) {
    this.barrier = barrier;
    this.taskConfiguration = taskConfiguration;
    this.workingDirectory = workingDirectory;
    this.archiveBuilder = archiveBuilder;
  }

  @Override
  public void run() {
    if (barrier != null) {
      LOG.info("Task {} started, waiting for the others to finish", taskConfiguration.name);
      try {
        barrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        LOG.error("Exception while waiting other tasks", e);
      }
    }

    LOG.info("[{}] Start downloading data", taskConfiguration.name);
    CommandLine cmd = new CommandLine("curl");
    cmd.addArgument(taskConfiguration.requestUrl);
    cmd.addArgument("-o");
    cmd.addArgument(taskConfiguration.rawDataFile);

    DefaultExecutor executor = new DefaultExecutor();
    executor.setExitValue(0);

    try {
      // download data
      executor.execute(cmd);
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
          taskConfiguration.metadataFile);
      LOG.info("[{}] Archive {} was created", taskConfiguration.name, archiveName);

      // delete temp files
      Files.deleteIfExists(Paths.get(taskConfiguration.rawDataFile));
      LOG.info(
          "[{}] Raw data file {} deleted", taskConfiguration.name, taskConfiguration.rawDataFile);
    } catch (IOException e) {
      LOG.error("[{}] IOException while producing archive", taskConfiguration.name, e);
    } catch (SQLException e) {
      LOG.error("[{}] SQLException while producing archive", taskConfiguration.name, e);
    }
  }

  protected abstract String prepareRawData() throws IOException, SQLException;
}
