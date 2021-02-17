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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveGeneratorFileSourceTask extends ArchiveGeneratorTask {

  private static final Logger LOG =
      LoggerFactory.getLogger(ArchiveGeneratorDatabaseSourceTask.class);

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
