package org.gbif.embl.cli;

import org.gbif.embl.util.DwcArchiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

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

  private static final Logger LOG = LoggerFactory.getLogger(ArchiveGeneratorDatabaseSourceTask.class);

  private final DataSource dataSource;

  public ArchiveGeneratorDatabaseSourceTask(
      String taskName,
      DataSource dataSource,
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
    this.dataSource = dataSource;
  }

  protected String prepareRawData() throws IOException, SQLException {
    LOG.debug("Database raw data");
    // store data to DB
    try (Connection connection = dataSource.getConnection();
         Statement st = connection.createStatement();
         PreparedStatement ps = connection.prepareStatement(SQL_INSERT);
         BufferedReader in = new BufferedReader(new FileReader(getRawDataFileName()))) {
      LOG.debug("Start writing DB");
      connection.setAutoCommit(false);

      // clean database table before
      st.executeUpdate(SQL_CLEAN);

      // skip first header line
      for (Iterator<String> it = in.lines().skip(1).iterator(); it.hasNext(); ) {
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
        ps.addBatch();
      }

      ps.executeBatch();
      connection.commit();
      LOG.debug("Finish writing DB");
      return "embl_data";
    }
  }
}
