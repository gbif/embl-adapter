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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.gbif.embl.util.EmblAdapterConstants.*;

public class DataGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DataGeneratorTask.class);

  private final DataSource dataSource;
  private final TaskConfiguration taskConfiguration;
  private final Marker marker;

  public DataGeneratorTask(TaskConfiguration taskConfiguration, DataSource dataSource) {
    this.taskConfiguration = taskConfiguration;
    this.dataSource = dataSource;
    this.marker = MarkerFactory.getMarker(taskConfiguration.name);
  }

  @Override
  public void run() {
    Thread.currentThread().setName(taskConfiguration.name);
    LOG.info("Start running task");

    LOG.info("Steps: {}", taskConfiguration.steps);

    try {
      // download data from URL
      downloadData();

      // store raw data into database
      storeData();

      // process raw data and store processed into database
      processData();

      // delete temp files
      deleteDataFiles();
    } catch (IOException e) {
      LOG.error("IOException while producing data", e);
    } catch (SQLException e) {
      LOG.error("SQLException while producing data", e);
    } catch (Exception e) {
      LOG.error("Exception while producing data", e);
    }
  }

  private void deleteDataFiles() throws IOException {
    if (taskConfiguration.steps.size() > 0
        && !taskConfiguration.steps.contains(TaskStep.DELETE_DATA_FILES)) {
      LOG.info(marker, "Skipping store data step");
      return;
    }

    Files.deleteIfExists(Paths.get(taskConfiguration.rawDataFile1));
    Files.deleteIfExists(Paths.get(taskConfiguration.rawDataFile2));
    LOG.info(marker, "Raw data file {} deleted", taskConfiguration.rawDataFile1);
    LOG.info(marker, "Raw data file {} deleted", taskConfiguration.rawDataFile2);
  }

  private void downloadData() throws IOException {
    if (taskConfiguration.steps.size() > 0
        && !taskConfiguration.steps.contains(TaskStep.DOWNLOAD_DATA)) {
      LOG.info(marker, "Skipping download data step");
      return;
    }

    LOG.info(marker, "Start downloading data");

    // download non-CON sequences
    String requestUrl1 =
        taskConfiguration.request1.url
            + "?dataPortal="
            + taskConfiguration.request1.dataPortal
            + "&result="
            + taskConfiguration.request1.result
            + "&offset="
            + taskConfiguration.request1.offset
            + "&limit="
            + taskConfiguration.request1.limit
            + "&fields="
            + taskConfiguration.request1.fields
            + "&query="
            + URLEncoder.encode(taskConfiguration.request1.query, StandardCharsets.UTF_8.name());

    LOG.debug("Downloading {}", requestUrl1);
    URL download = new URL(requestUrl1);
    Files.copy(download.openStream(), Paths.get(taskConfiguration.rawDataFile1));

    // download wgs_set
    String requestUrl2 =
        taskConfiguration.request2.url
            + "?dataPortal="
            + taskConfiguration.request2.dataPortal
            + "&result="
            + taskConfiguration.request2.result
            + "&offset="
            + taskConfiguration.request2.offset
            + "&limit="
            + taskConfiguration.request2.limit
            + "&fields="
            + taskConfiguration.request2.fields
            + "&query="
            + URLEncoder.encode(taskConfiguration.request2.query, StandardCharsets.UTF_8.name());

    LOG.debug("Downloading {}", requestUrl2);
    URL download2 = new URL(requestUrl2);
    Files.copy(download2.openStream(), Paths.get(taskConfiguration.rawDataFile2));

    LOG.debug("Download complete.");
  }

  protected void storeData() throws IOException, SQLException {
    if (taskConfiguration.steps.size() > 0
        && !taskConfiguration.steps.contains(TaskStep.STORE_DATA)) {
      LOG.info(marker, "Skipping store data step");
      return;
    }

    LOG.debug(marker, "Store raw data into DB");
    String sqlInsert = SQL_INSERT_RAW_DATA.replace("embl_data", taskConfiguration.tableName);
    String sqlClean = SQL_CLEAN.replace("embl_data", taskConfiguration.tableName);
    String sqlTestSelect = SQL_TEST_SELECT.replace("embl_data", taskConfiguration.tableName);

    // store data to DB
    try (Connection connection = dataSource.getConnection();
        Statement st = connection.createStatement();
        PreparedStatement ps = connection.prepareStatement(sqlInsert);
        Statement test = connection.createStatement();
        BufferedReader fileReader1 =
            new BufferedReader(new FileReader(taskConfiguration.rawDataFile1));
        BufferedReader fileReader2 =
            new BufferedReader(new FileReader(taskConfiguration.rawDataFile2))) {

      // test table is fine and all columns are present
      test.execute(sqlTestSelect);

      // begin transaction
      connection.setAutoCommit(false);

      // clean database table before
      st.executeUpdate(sqlClean);
      LOG.debug(marker, "DB cleaned");

      LOG.debug(marker, "Start writing DB");

      executeBatch(ps, fileReader1, false);
      executeBatch(ps, fileReader2, true);

      // complete transaction
      connection.commit();
      LOG.debug(marker, "Finish writing DB");
    }
  }

  private void executeBatch(
      PreparedStatement ps, BufferedReader fileReader, boolean skipSequenceMd5)
      throws SQLException {
    int lineNumber = 0;

    int expectedAmountOfParameters = StringUtils.countMatches(SQL_INSERT_RAW_DATA, '?');
    int expectedAmountOfColumns = StringUtils.split(SQL_COLUMNS_RAW_DATA, ",").length;

    if (expectedAmountOfParameters != RAW_MAX_INDEX
        || expectedAmountOfParameters != expectedAmountOfColumns) {
      throw new IllegalStateException(
          "Numbers of parameters do not match! Check query and configuration");
    }

    Map<String, Integer> columnMapping = new HashMap<>();
    for (Iterator<String> it = fileReader.lines().iterator(); it.hasNext(); lineNumber++) {
      String line = it.next();
      String[] split = line.split(DEFAULT_DELIMITER, -1);
      if (split.length < 14) {
        LOG.error(marker, "Must be at least 14 columns! Found {}", split.length);
        continue;
      }

      if (columnMapping.isEmpty()) {
        // Determine the mapping from the header line. It may change!
        for (int i = 0; i < split.length; i++) {
          columnMapping.put(split[i], i);
        }
        continue;
      }

      ps.setString(RAW_INDEX_ACCESSION, split[columnMapping.get(ACCESSION_COLUMN)]);
      ps.setString(RAW_INDEX_SAMPLE_ACCESSION, split[columnMapping.get(SAMPLE_ACCESSION_COLUMN)]);
      ps.setString(RAW_INDEX_LOCATION, split[columnMapping.get(LOCATION_COLUMN)]);
      ps.setString(RAW_INDEX_COUNTRY, split[columnMapping.get(COUNTRY_COLUMN)]);
      ps.setString(RAW_INDEX_IDENTIFIED_BY, split[columnMapping.get(IDENTIFIED_BY_COLUMN)]);
      ps.setString(RAW_INDEX_COLLECTED_BY, split[columnMapping.get(COLLECTED_BY_COLUMN)]);
      ps.setString(RAW_INDEX_COLLECTION_DATE, split[columnMapping.get(COLLECTION_DATE_COLUMN)]);
      ps.setString(RAW_INDEX_SPECIMEN_VOUCHER, split[columnMapping.get(SPECIMEN_VOUCHER_COLUMN)]);
      ps.setString(
          RAW_INDEX_SEQUENCE_MD5,
          skipSequenceMd5 ? "" : split[columnMapping.get(SEQUENCE_MD5_COLUMN)]);
      ps.setString(RAW_INDEX_SCIENTIFIC_NAME, split[columnMapping.get(SCIENTIFIC_NAME_COLUMN)]);
      ps.setString(RAW_INDEX_TAX_ID, split[columnMapping.get(TAX_ID_COLUMN)]);
      ps.setString(RAW_INDEX_ALTITUDE, split[columnMapping.get(ALTITUDE_COLUMN)]);
      ps.setString(RAW_INDEX_SEX, split[columnMapping.get(SEX_COLUMN)]);
      ps.setString(RAW_INDEX_DESCRIPTION, split[columnMapping.get(DESCRIPTION_COLUMN)]);
      ps.setString(RAW_INDEX_HOST, split[columnMapping.get(HOST_COLUMN)]);
      ps.addBatch();

      if (lineNumber % WRITE_BATCH_SIZE == 0) {
        ps.executeBatch();
      }
    }

    ps.executeBatch();
  }

  private void processData() throws SQLException {
    if (taskConfiguration.steps.size() > 0
        && !taskConfiguration.steps.contains(TaskStep.PROCESS_DATA)) {
      LOG.info(marker, "Skipping store data step");
      return;
    }

    String tableName = taskConfiguration.tableName;
    String query = taskConfiguration.query;
    LOG.info(marker, "Start processing raw data {} ", tableName);

    processRawDataInternal(tableName, query);
  }

  private void processRawDataInternal(String tableName, String query) throws SQLException {
    LOG.debug(marker, "Processing raw data from database");

    int lineNumber = 0;
    int linesSkipped = 0;
    Set<String> recordsSeenBefore = new HashSet<>();

    // SQL select for table
    String sqlSelectRawData = readSqlFile(query).replace("embl_data", tableName).trim();
    LOG.debug(marker, "SQL select (raw data): {}", sqlSelectRawData);

    String sqlInsertProcessedData =
        SQL_INSERT_PROCESSED_DATA.replace("embl_data", tableName + "_processed");
    String sqlCleanProcessedData = SQL_CLEAN.replace("embl_data", tableName + "_processed");
    LOG.debug(marker, "SQL insert (processed data): {}", sqlInsertProcessedData);

    try (Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection()) {
      LOG.debug(marker, "DB connection established to retrieve raw data");
      connection1.setAutoCommit(false);
      connection2.setAutoCommit(false);

      try (Statement s = connection1.createStatement();
          PreparedStatement ps = connection2.prepareStatement(sqlInsertProcessedData)) {
        // set batch size
        ps.setFetchSize(WRITE_BATCH_SIZE);
        s.setFetchSize(READ_BATCH_SIZE);

        // clean processed data before
        s.executeUpdate(sqlCleanProcessedData);
        connection1.commit();
        LOG.debug(marker, "Processed data DB cleaned");

        try (ResultSet rs = s.executeQuery(sqlSelectRawData)) {
          LOG.debug(marker, "Start writing processed data");

          // processed data
          while (rs.next()) {
            // skip records with missing specimen_voucher and collection_date
            if (StringUtils.isEmpty(getSpecimenVoucher(rs))
                && StringUtils.isEmpty(getCollectionDate(rs))) {
              linesSkipped++;
              continue;
            }

            // check if the record was seen before
            if (StringUtils.isNotEmpty(getSampleAccession(rs))
                && StringUtils.isNotEmpty(getScientificName(rs))) {
              String sampleAccessionPlusScientificName =
                  getSampleAccession(rs) + getScientificName(rs);

              // skip duplicate records (seen before) based on sample_accession and scientific_name
              // otherwise remember and write it
              if (recordsSeenBefore.contains(sampleAccessionPlusScientificName)) {
                linesSkipped++;
                continue;
              } else {
                recordsSeenBefore.add(sampleAccessionPlusScientificName);
              }
            }

            // process raw data
            lineNumber++;
            prepareLine(rs, ps);

            if (lineNumber % WRITE_BATCH_SIZE == 0) {
              ps.executeBatch();
            }
          }

          // execute last records
          ps.executeBatch();
        }
      }

      connection2.commit();
      LOG.debug(marker, "Raw data processing finished, processed data stored.");
      LOG.debug(marker, "Lines processed: {}. Lines skipped: {}", lineNumber, linesSkipped);
    }
  }

  private void prepareLine(ResultSet rs, PreparedStatement ps) throws SQLException {
    // occurrenceID term
    setOccurrenceId(ps, trimToEmpty(getAccession(rs)));
    // associatedSequences term
    setAssociatedSequences(ps, toAssociatedSequences(getAccession(rs)));
    // references term
    setReferences(ps, toReferences(getAccession(rs)));
    // decimalLatitude term
    setDecimalLatitude(ps, toLatitude(getLocation(rs)));
    // decimalLongitude term
    setDecimalLongitude(ps, toLongitude(getLocation(rs)));
    // country term
    setCountry(ps, toCountry(getCountry(rs)));
    // locality term
    setLocality(ps, toLocality(getCountry(rs)));
    // identifiedBy term
    setIdentifiedBy(ps, trimToEmpty(getIdentifiedBy(rs)));
    // recordedBy term
    setRecordedBy(ps, trimToEmpty(getCollectedBy(rs)));
    // eventDate term
    setEventDate(ps, trimToEmpty(getCollectionDate(rs)));
    // catalogNumber term
    setCatalogNumber(ps, trimToEmpty(getSpecimenVoucher(rs)));
    // basisOfRecord term
    setBasisOfRecord(ps, toBasisOfRecord(getSpecimenVoucher(rs)));
    // taxonID term
    setTaxonId(ps, toTaxonId(getSequenceMd5(rs)));
    // scientificName term
    setScientificName(ps, trimToEmpty(getScientificName(rs)));
    // taxonConceptID term
    setTaxonConceptId(ps, toTaxonConceptId(getTaxId(rs)));
    // minimumElevationInMeters term
    setMinimumElevation(ps, trimToEmpty(getAltitude(rs)));
    // maximumElevationInMeters term
    setMaximumElevation(ps, trimToEmpty(getAltitude(rs)));
    // sex term
    setSex(ps, trimToEmpty(getSex(rs)));
    // occurrenceRemarks term
    setOccurrenceRemarks(ps, trimToEmpty(getDescription(rs)));
    // associatedTaxa term
    setAssociatedTaxa(ps, trimToEmpty(getHost(rs)));
    // kingdom term
    setKingdom(ps, trimToEmpty(getKingdom(rs)));
    // phylum term
    setPhylum(ps, trimToEmpty(getPhylum(rs)));
    // class term
    setClass(ps, trimToEmpty(getClass(rs)));
    // order term
    setOrder(ps, trimToEmpty(getOrder(rs)));
    // family term
    setFamily(ps, trimToEmpty(getFamily(rs)));
    // genus term
    setGenus(ps, trimToEmpty(getGenus(rs)));

    ps.addBatch();
  }

  private String readSqlFile(String filePath) {
    LOG.debug(marker, "Start reading SQL file {}", filePath);
    StringBuilder sb = new StringBuilder();

    try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
      stream.forEach(s -> sb.append(s).append("\n"));
    } catch (IOException e) {
      LOG.error(marker, "Exception while reading SQL file {}", filePath);
      throw new RuntimeException(e);
    }

    LOG.debug(marker, "Finished reading SQL file {}", filePath);
    return sb.toString();
  }

  @SuppressWarnings("SameParameterValue")
  private String getOrEmpty(String[] arr, int index) {
    return arr.length > index ? arr[index] : StringUtils.EMPTY;
  }

  private String toTaxonConceptId(String data) {
    return StringUtils.isNotBlank(data) ? TAXON_CONCEPT_ID_URL + data : StringUtils.EMPTY;
  }

  private String toReferences(String data) {
    return StringUtils.isNotBlank(data) ? REFERENCES_URL + data : StringUtils.EMPTY;
  }

  private String toAssociatedSequences(String data) {
    return StringUtils.isNotBlank(data) ? ASSOCIATED_SEQUENCES_URL + data : StringUtils.EMPTY;
  }

  private String toTaxonId(String data) {
    return StringUtils.isNotBlank(data) ? TAXON_ID_PREFIX + data : StringUtils.EMPTY;
  }

  private String toBasisOfRecord(String data) {
    return StringUtils.isNotBlank(data) ? PRESERVED_SPECIMEN : MATERIAL_SAMPLE;
  }

  private String toCountry(String country) {
    if (StringUtils.isNotBlank(country)) {
      return country.contains(COUNTRY_DELIMITER) ? country.split(COUNTRY_DELIMITER)[0] : country;
    }

    return StringUtils.EMPTY;
  }

  private String toLocality(String country) {
    if (StringUtils.isNotBlank(country) && country.contains(COUNTRY_DELIMITER)) {
      return getOrEmpty(country.split(COUNTRY_DELIMITER, -1), 1).trim();
    }

    return StringUtils.EMPTY;
  }

  private String toLatitude(String location) {
    if (StringUtils.isNotBlank(location)) {
      Matcher matcher = LOCATION_PATTERN.matcher(location);
      if (matcher.find()) {
        // south - negative
        if (SOUTH.equals(matcher.group(2))) {
          return "-" + matcher.group(1);
        }
        // north - positive
        else if (NORTH.equals(matcher.group(2))) {
          return matcher.group(1);
        }
        // wrong letter - log error, return empty value
        else {
          LOG.error(marker, "Wrong coordinate letter: {}", matcher.group(2));
          return StringUtils.EMPTY;
        }
      } else {
        LOG.error(marker, "Coordinates {} do not match pattern", location);
      }
    }
    return StringUtils.EMPTY;
  }

  private String toLongitude(String location) {
    if (StringUtils.isNotBlank(location)) {
      Matcher matcher = LOCATION_PATTERN.matcher(location);
      if (matcher.find()) {
        // west - negative
        if (WEST.equals(matcher.group(4))) {
          return "-" + matcher.group(3);
        }
        // east - positive
        else if (EAST.equals(matcher.group(4))) {
          return matcher.group(3);
        }
        // wrong letter - log error, return empty value
        else {
          LOG.error(marker, "Wrong coordinate letter: {}", matcher.group(4));
          return StringUtils.EMPTY;
        }
      } else {
        LOG.error(marker, "Coordinates {} do not match pattern", location);
      }
    }
    return StringUtils.EMPTY;
  }

  private String getAccession(ResultSet rs) throws SQLException {
    return rs.getString(ACCESSION_COLUMN);
  }

  private String getSampleAccession(ResultSet rs) throws SQLException {
    return rs.getString(SAMPLE_ACCESSION_COLUMN);
  }

  private String getLocation(ResultSet rs) throws SQLException {
    return rs.getString(LOCATION_COLUMN);
  }

  private String getCountry(ResultSet rs) throws SQLException {
    return rs.getString(COUNTRY_COLUMN);
  }

  private String getIdentifiedBy(ResultSet rs) throws SQLException {
    return rs.getString(IDENTIFIED_BY_COLUMN);
  }

  private String getCollectedBy(ResultSet rs) throws SQLException {
    return rs.getString(COLLECTED_BY_COLUMN);
  }

  private String getCollectionDate(ResultSet rs) throws SQLException {
    return rs.getString(COLLECTION_DATE_COLUMN);
  }

  private String getSpecimenVoucher(ResultSet rs) throws SQLException {
    return rs.getString(SPECIMEN_VOUCHER_COLUMN);
  }

  private String getSequenceMd5(ResultSet rs) throws SQLException {
    return rs.getString(SEQUENCE_MD5_COLUMN);
  }

  private String getScientificName(ResultSet rs) throws SQLException {
    return rs.getString(SCIENTIFIC_NAME_COLUMN);
  }

  private String getTaxId(ResultSet rs) throws SQLException {
    return rs.getString(TAX_ID_COLUMN);
  }

  private String getAltitude(ResultSet rs) throws SQLException {
    return rs.getString(ALTITUDE_COLUMN);
  }

  private String getSex(ResultSet rs) throws SQLException {
    return rs.getString(SEX_COLUMN);
  }

  private String getDescription(ResultSet rs) throws SQLException {
    return rs.getString(DESCRIPTION_COLUMN);
  }

  private String getHost(ResultSet rs) throws SQLException {
    return rs.getString(HOST_COLUMN);
  }

  private String getKingdom(ResultSet rs) throws SQLException {
    return rs.getString(KINGDOM_COLUMN);
  }

  private String getPhylum(ResultSet rs) throws SQLException {
    return rs.getString(PHYLUM_COLUMN);
  }

  private String getClass(ResultSet rs) throws SQLException {
    return rs.getString(CLASS_COLUMN);
  }

  private String getOrder(ResultSet rs) throws SQLException {
    return rs.getString(ORDER_COLUMN);
  }

  private String getFamily(ResultSet rs) throws SQLException {
    return rs.getString(FAMILY_COLUMN);
  }

  private String getGenus(ResultSet rs) throws SQLException {
    return rs.getString(GENUS_COLUMN);
  }

  private void setOccurrenceId(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_OCCURRENCE_ID, data);
  }

  private void setAssociatedSequences(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_ASSOCIATED_SEQUENCES, data);
  }

  private void setReferences(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_REFERENCES, data);
  }

  private void setDecimalLatitude(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_DECIMAL_LATITUDE, data);
  }

  private void setDecimalLongitude(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_DECIMAL_LONGITUDE, data);
  }

  private void setCountry(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_COUNTRY, data);
  }

  private void setLocality(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_LOCALITY, data);
  }

  private void setIdentifiedBy(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_IDENTIFIED_BY, data);
  }

  private void setRecordedBy(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_RECORDED_BY, data);
  }

  private void setEventDate(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_EVENT_DATE, data);
  }

  private void setCatalogNumber(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_CATALOG_NUMBER, data);
  }

  private void setBasisOfRecord(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_BASIS_OF_RECORD, data);
  }

  private void setTaxonId(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_TAXON_ID, data);
  }

  private void setScientificName(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_SCIENTIFIC_NAME, data);
  }

  private void setTaxonConceptId(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_TAXON_CONCEPT_ID, data);
  }

  private void setMinimumElevation(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_MINIMUM_ELEVATION_IN_METERS, data);
  }

  private void setMaximumElevation(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_MAXIMUM_ELEVATION_IN_METERS, data);
  }

  private void setSex(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_SEX, data);
  }

  private void setOccurrenceRemarks(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_OCCURRENCE_REMARK, data);
  }

  private void setAssociatedTaxa(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_ASSOCIATED_TAXA, data);
  }

  private void setKingdom(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_KINGDOM, data);
  }

  private void setPhylum(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_PHYLUM, data);
  }

  private void setClass(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_CLASS, data);
  }

  private void setOrder(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_ORDER, data);
  }

  private void setFamily(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_FAMILY, data);
  }

  private void setGenus(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PROCESSED_INDEX_GENUS, data);
  }
}
