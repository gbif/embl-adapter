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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.embl.util.EmblAdapterConstants.ACCESSION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ACCESSION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ALTITUDE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ALTITUDE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTED_BY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTION_DATE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTION_DATE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DEFAULT_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SCIENTIFIC_NAME_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SCIENTIFIC_NAME_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEQUENCE_MD5_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEQUENCE_MD5_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEX_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEX_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SPECIMEN_VOUCHER_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SPECIMEN_VOUCHER_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SQL_CLEAN;
import static org.gbif.embl.util.EmblAdapterConstants.SQL_INSERT;
import static org.gbif.embl.util.EmblAdapterConstants.TAX_ID_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.TAX_ID_RS_INDEX;

public class ArchiveGeneratorDatabaseSourceTask extends ArchiveGeneratorTask {

  private static final Logger LOG =
      LoggerFactory.getLogger(ArchiveGeneratorDatabaseSourceTask.class);

  public static final int BATCH_SIZE = 5000;

  private final DataSource dataSource;
  private final TaskConfiguration taskConfiguration;

  public ArchiveGeneratorDatabaseSourceTask(
      TaskConfiguration taskConfiguration,
      DataSource dataSource,
      String workingDirectory,
      DwcArchiveBuilder archiveBuilder) {
    super(taskConfiguration, workingDirectory, archiveBuilder);
    this.taskConfiguration = taskConfiguration;
    this.dataSource = dataSource;
  }

  protected String prepareRawData() throws IOException, SQLException {
    LOG.debug("Database raw data");
    String sqlInsert = SQL_INSERT.replace("embl_data", taskConfiguration.tableName);
    String sqlClean = SQL_CLEAN.replace("embl_data", taskConfiguration.tableName);
    // store data to DB
    try (Connection connection = dataSource.getConnection();
        Statement st = connection.createStatement();
        PreparedStatement ps = connection.prepareStatement(sqlInsert);
        BufferedReader in = new BufferedReader(new FileReader(taskConfiguration.rawDataFile))) {
      LOG.debug("Start writing DB");

      // clean database table before
      st.executeUpdate(sqlClean);
      LOG.debug("DB cleaned");

      int lineNumber = 0;

      // skip first header line
      for (Iterator<String> it = in.lines().skip(1).iterator(); it.hasNext(); lineNumber++) {
        String line = it.next();
        String[] split = line.split(DEFAULT_DELIMITER, -1);
        ps.setString(ACCESSION_RS_INDEX, split[ACCESSION_INDEX]);
        ps.setString(LOCATION_RS_INDEX, split[LOCATION_INDEX]);
        ps.setString(COUNTRY_RS_INDEX, split[COUNTRY_INDEX]);
        ps.setString(IDENTIFIED_BY_RS_INDEX, split[IDENTIFIED_BY_INDEX]);
        ps.setString(COLLECTED_BY_RS_INDEX, split[COLLECTED_BY_INDEX]);
        ps.setString(COLLECTION_DATE_RS_INDEX, split[COLLECTION_DATE_INDEX]);
        ps.setString(SPECIMEN_VOUCHER_RS_INDEX, split[SPECIMEN_VOUCHER_INDEX]);
        ps.setString(SEQUENCE_MD5_RS_INDEX, split[SEQUENCE_MD5_INDEX]);
        ps.setString(SCIENTIFIC_NAME_RS_INDEX, split[SCIENTIFIC_NAME_INDEX]);
        ps.setString(TAX_ID_RS_INDEX, split[TAX_ID_INDEX]);
        ps.setString(ALTITUDE_RS_INDEX, split[ALTITUDE_INDEX]);
        ps.setString(SEX_RS_INDEX, split[SEX_INDEX]);
        ps.setString(DESCRIPTION_RS_INDEX, split[DESCRIPTION_INDEX]);
        ps.addBatch();

        if (lineNumber % BATCH_SIZE == 0) {
          ps.executeBatch();
        }
      }

      ps.executeBatch();
      LOG.debug("Finish writing DB");

      return taskConfiguration.tableName;
    }
  }
}
