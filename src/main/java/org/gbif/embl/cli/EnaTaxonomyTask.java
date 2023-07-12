/*
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

import org.gbif.dwc.Archive;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.gbif.embl.util.EmblAdapterConstants.*;

public class EnaTaxonomyTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(EnaTaxonomyTask.class);

  private final TaxonomyConfiguration taxonomyConfig;
  private final DataSource dataSource;

  public EnaTaxonomyTask(TaxonomyConfiguration taxonomyConfig, DataSource dataSource) {
    this.taxonomyConfig = taxonomyConfig;
    this.dataSource = dataSource;
  }

  @Override
  public void run() {
    try (Connection connection = dataSource.getConnection();
        Statement st = connection.createStatement();
        PreparedStatement ps = connection.prepareStatement(SQL_INSERT_TAXONOMY)) {
      LOG.debug("{}", taxonomyConfig);

      // create tempDir if not exists
      File tempDir = new File(taxonomyConfig.tempDir);
      if (!tempDir.exists()) {
        LOG.debug("Directory {} does not exist, creating", taxonomyConfig.tempDir);
        boolean result = tempDir.mkdir();
        LOG.debug("Directory {} created: {}", taxonomyConfig.tempDir, result);
      }

      // download archive
      downloadTaxonomyArchive();

      Path archiveFile = Paths.get(taxonomyConfig.tempDir, taxonomyConfig.archiveName);
      Path extractToFolder = Paths.get(taxonomyConfig.extractedDir);
      Archive dwcArchive = DwcFiles.fromCompressed(archiveFile, extractToFolder);

      // clean database table before
      st.executeUpdate(SQL_CLEAN_TAXONOMY);
      LOG.debug("Taxonomy DB cleaned");

      // store data to DB
      LOG.debug("Start writing taxonomy to DB table {}", TAXONOMY_TABLE);

      int lineNumber = 0;

      for (Record rec : dwcArchive.getCore()) {
        lineNumber++;
        if (StringUtils.isEmpty(rec.value(DwcTerm.taxonID))) {
          LOG.warn("Missing taxonID line {}!", lineNumber);
          continue;
        }
        ps.setString(TAXONOMY_INDEX_TAXON_ID, rec.value(DwcTerm.taxonID));
        ps.setString(TAXONOMY_INDEX_KINGDOM, trimToEmpty(rec.value(DwcTerm.kingdom)));
        ps.setString(TAXONOMY_INDEX_PHYLUM, trimToEmpty(rec.value(DwcTerm.phylum)));
        ps.setString(TAXONOMY_INDEX_CLASS, trimToEmpty(rec.value(DwcTerm.class_)));
        ps.setString(TAXONOMY_INDEX_ORDER, trimToEmpty(rec.value(DwcTerm.order)));
        ps.setString(TAXONOMY_INDEX_FAMILY, trimToEmpty(rec.value(DwcTerm.family)));
        ps.setString(TAXONOMY_INDEX_GENUS, trimToEmpty(rec.value(DwcTerm.genus)));
        ps.addBatch();

        if (lineNumber % WRITE_BATCH_SIZE == 0) {
          ps.executeBatch();
        }
      }

      ps.executeBatch();
      LOG.debug("Finish writing taxonomy to DB");
    } catch (IOException | SQLException e) {
      LOG.error("Error while processing taxonomy", e);
      throw new RuntimeException(e);
    } finally {
      LOG.debug("Cleaning up taxonomy temp directory {}", taxonomyConfig.tempDir);
      File file = new File(taxonomyConfig.tempDir);
      if (file.exists()) {
        FileUtils.deleteDirectoryRecursively(file);
      }
    }
  }

  private void downloadTaxonomyArchive() throws IOException {
    LOG.debug("Start downloading taxonomy archive from {}", taxonomyConfig.archiveUrl);

    URLConnection urlConnection = new URL(taxonomyConfig.archiveUrl).openConnection();
    try (InputStream inputStream = urlConnection.getInputStream()) {
      Files.copy(
          inputStream, new File(taxonomyConfig.tempDir, taxonomyConfig.archiveName).toPath());
    }

    LOG.debug(
        "Taxonomy archive downloaded: {}",
        new File(taxonomyConfig.tempDir, taxonomyConfig.archiveName).exists());
  }
}
