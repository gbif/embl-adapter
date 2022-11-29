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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.gbif.embl.util.EmblAdapterConstants.ACCESSION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ACCESSION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ALTITUDE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ALTITUDE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ASSOCIATED_SEQUENCES_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ASSOCIATED_SEQUENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.ASSOCIATED_TAXA_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.BASIS_OF_RECORD_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.BATCH_SIZE;
import static org.gbif.embl.util.EmblAdapterConstants.CATALOG_NUMBER_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.CLASS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.CLASS_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTED_BY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTION_DATE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTION_DATE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_PROCESSED_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DECIMAL_LATITUDE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DECIMAL_LONGITUDE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DEFAULT_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.EAST;
import static org.gbif.embl.util.EmblAdapterConstants.EVENT_DATE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.FAMILY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.FAMILY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.GENUS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.GENUS_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.HOST_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.HOST_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_PROCESSED_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.KINGDOM_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.KINGDOM_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCALITY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_PATTERN;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.MATERIAL_SAMPLE;
import static org.gbif.embl.util.EmblAdapterConstants.MAXIMUM_ELEVATION_IN_METERS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.MINIMUM_ELEVATION_IN_METERS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.NORTH;
import static org.gbif.embl.util.EmblAdapterConstants.OCCURRENCE_ID_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.OCCURRENCE_REMARK_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ORDER_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ORDER_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.PHYLUM_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.PHYLUM_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.PRESERVED_SPECIMEN;
import static org.gbif.embl.util.EmblAdapterConstants.RECORDED_BY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.REFERENCES_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.REFERENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.RS_MAX_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SAMPLE_ACCESSION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SAMPLE_ACCESSION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SCIENTIFIC_NAME_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SCIENTIFIC_NAME_PROCESSED_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SCIENTIFIC_NAME_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEQUENCE_MD5_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEQUENCE_MD5_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEX_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEX_PROCESSED_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEX_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SOUTH;
import static org.gbif.embl.util.EmblAdapterConstants.SPECIMEN_VOUCHER_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SPECIMEN_VOUCHER_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SQL_CLEAN;
import static org.gbif.embl.util.EmblAdapterConstants.SQL_COLUMNS_RAW_DATA;
import static org.gbif.embl.util.EmblAdapterConstants.SQL_INSERT_PROCESSED_DATA;
import static org.gbif.embl.util.EmblAdapterConstants.SQL_INSERT_RAW_DATA;
import static org.gbif.embl.util.EmblAdapterConstants.SQL_TEST_SELECT;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_CONCEPT_ID_URL;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_CONCEPT_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_ID_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_ID_PREFIX;
import static org.gbif.embl.util.EmblAdapterConstants.TAX_ID_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.TAX_ID_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.WEST;

public class DataGeneratorTask implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DataGeneratorTask.class);

  private final DataSource dataSource;
  private final TaskConfiguration taskConfiguration;

  public DataGeneratorTask(TaskConfiguration taskConfiguration, DataSource dataSource) {
    this.taskConfiguration = taskConfiguration;
    this.dataSource = dataSource;
  }

  @Override
  public void run() {
    Thread.currentThread().setName(taskConfiguration.name);
    LOG.info("Start running task");

    // download non-CON sequences
    CommandLine downloadSequencesCommand = new CommandLine("curl");

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
            + taskConfiguration.request1.query;

    downloadSequencesCommand.addArgument(requestUrl1);
    downloadSequencesCommand.addArgument("-o");
    downloadSequencesCommand.addArgument(taskConfiguration.rawDataFile1);

    // download wgs_set
    CommandLine downloadWgsSetCommand = new CommandLine("curl");

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
            + taskConfiguration.request2.query;

    downloadWgsSetCommand.addArgument(requestUrl2);
    downloadWgsSetCommand.addArgument("-o");
    downloadWgsSetCommand.addArgument(taskConfiguration.rawDataFile2);

    DefaultExecutor executor = new DefaultExecutor();
    executor.setExitValue(0);

    try {
      // download data
      LOG.info("Start downloading data");
      executor.execute(downloadSequencesCommand);
      executor.execute(downloadWgsSetCommand);

      // store raw data into database
      String tableName = prepareRawData();

      // process raw data and store processed into database
      LOG.info("Start processing {}", tableName);
      processRawData(tableName, taskConfiguration.query);
      LOG.info("Finished processing {}", tableName);

      // delete temp files
      Files.deleteIfExists(Paths.get(taskConfiguration.rawDataFile1));
      Files.deleteIfExists(Paths.get(taskConfiguration.rawDataFile2));
      LOG.info("Raw data file {} deleted", taskConfiguration.rawDataFile1);
      LOG.info("Raw data file {} deleted", taskConfiguration.rawDataFile2);
    } catch (IOException e) {
      LOG.error("IOException while producing data", e);
    } catch (SQLException e) {
      LOG.error("SQLException while producing data", e);
    } catch (Exception e) {
      LOG.error("Exception while producing data", e);
    }
  }

  protected String prepareRawData() throws IOException, SQLException {
    LOG.debug("Database raw data");
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

      // clean database table before
      st.executeUpdate(sqlClean);
      LOG.debug("DB cleaned");

      LOG.debug("Start writing DB");

      executeBatch(ps, fileReader1, false);
      executeBatch(ps, fileReader2, true);

      LOG.debug("Finish writing DB");

      return taskConfiguration.tableName;
    }
  }

  private void executeBatch(
      PreparedStatement ps, BufferedReader fileReader, boolean skipSequenceMd5)
      throws SQLException {
    int lineNumber = 0;

    int expectedAmountOfParameters = StringUtils.countMatches(SQL_INSERT_RAW_DATA, '?');
    int expectedAmountOfColumns = StringUtils.split(SQL_COLUMNS_RAW_DATA, ",").length;

    if (expectedAmountOfParameters != RS_MAX_INDEX
        || expectedAmountOfParameters != expectedAmountOfColumns) {
      throw new IllegalStateException(
          "Numbers of parameters do not match! Check query and configuration");
    }

    // skip first header line
    for (Iterator<String> it = fileReader.lines().skip(1).iterator(); it.hasNext(); lineNumber++) {
      String line = it.next();
      String[] split = line.split(DEFAULT_DELIMITER, -1);
      if (split.length < 14) {
        LOG.error("Must be at least 14 columns! Found {}", split.length);
        continue;
      }
      ps.setString(ACCESSION_RS_INDEX, split[ACCESSION_INDEX]);
      ps.setString(SAMPLE_ACCESSION_RS_INDEX, split[SAMPLE_ACCESSION_INDEX]);
      ps.setString(LOCATION_RS_INDEX, split[LOCATION_INDEX]);
      ps.setString(COUNTRY_RS_INDEX, split[COUNTRY_INDEX]);
      ps.setString(IDENTIFIED_BY_RS_INDEX, split[IDENTIFIED_BY_INDEX]);
      ps.setString(COLLECTED_BY_RS_INDEX, split[COLLECTED_BY_INDEX]);
      ps.setString(COLLECTION_DATE_RS_INDEX, split[COLLECTION_DATE_INDEX]);
      ps.setString(SPECIMEN_VOUCHER_RS_INDEX, split[SPECIMEN_VOUCHER_INDEX]);
      ps.setString(SEQUENCE_MD5_RS_INDEX, skipSequenceMd5 ? "" : split[SEQUENCE_MD5_INDEX]);
      ps.setString(SCIENTIFIC_NAME_RS_INDEX, split[SCIENTIFIC_NAME_INDEX]);
      ps.setString(TAX_ID_RS_INDEX, split[TAX_ID_INDEX]);
      ps.setString(ALTITUDE_RS_INDEX, split[ALTITUDE_INDEX]);
      ps.setString(SEX_RS_INDEX, split[SEX_INDEX]);
      ps.setString(DESCRIPTION_RS_INDEX, split[DESCRIPTION_INDEX]);
      ps.setString(HOST_RS_INDEX, split[HOST_INDEX]);
      ps.addBatch();

      if (lineNumber % BATCH_SIZE == 0) {
        ps.executeBatch();
      }
    }

    ps.executeBatch();
  }

  private void processRawData(String tableName, String query) {
    LOG.info("Start processing raw data {} ", tableName);

    try {
      processRawDataInternal(tableName, query);
    } catch (Exception e) {
      LOG.error("Error while processing raw data", e);
      throw new RuntimeException(e);
    }
  }

  private void processRawDataInternal(String tableName, String query) throws SQLException {
    LOG.debug("Processing raw data from database");

    int lineNumber = 0;
    int linesSkipped = 0;
    Set<String> recordsSeenBefore = new HashSet<>();

    // SQL select for table
    String sqlSelectRawData = readSqlFile(query).replace("embl_data", tableName).trim();
    LOG.debug("SQL select (raw data): {}", sqlSelectRawData);

    String sqlInsertProcessedData = SQL_INSERT_PROCESSED_DATA.replace("embl_data", tableName + "_processed");
    String sqlCleanProcessedData = SQL_CLEAN.replace("embl_data", tableName + "_processed");
    LOG.debug("SQL insert (processed data): {}", sqlInsertProcessedData);

    try (Connection connection = dataSource.getConnection()) {
      LOG.debug("DB connection established to retrieve raw data");
      connection.setAutoCommit(true);

      try (Statement s = connection.createStatement();
           PreparedStatement ps = connection.prepareStatement(sqlInsertProcessedData)) {
        // set batch size
        s.setFetchSize(BATCH_SIZE);

        // clean processed data before
        s.executeUpdate(sqlCleanProcessedData);

        try (ResultSet rs = s.executeQuery(sqlSelectRawData)) {
          LOG.debug("Start writing processed data");

          // processed data
          while (rs.next()) {
            // skip records with missing specimen_voucher and collection_date
            if (StringUtils.isEmpty(getSpecimenVoucher(rs)) && StringUtils.isEmpty(getCollectionDate(rs))) {
              linesSkipped++;
              continue;
            }

            // check if the record was seen before
            if (StringUtils.isNotEmpty(getSampleAccession(rs)) && StringUtils.isNotEmpty(getScientificName(rs))) {
              String sampleAccessionPlusScientificName = getSampleAccession(rs) + getScientificName(rs);

              // skip duplicate records (seen before) based on sample_accession and scientific_name
              // otherwise remember and write it
              if (recordsSeenBefore.contains(sampleAccessionPlusScientificName)) {
                linesSkipped++;
                continue;
              } else {
                recordsSeenBefore.add(sampleAccessionPlusScientificName);
              }
            }

            // process raw data and
            lineNumber++;
            prepareLine(rs, ps);

            if (lineNumber % BATCH_SIZE == 0) {
              ps.executeBatch();
            }
          }

          // execute last records
          ps.executeBatch();
        }
      }
      LOG.debug("Raw data processing finished, processed data stored.");
      LOG.debug("Lines processed: {}. Lines skipped: {}", lineNumber, linesSkipped);
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
    LOG.debug("Start reading SQL file {}", filePath);
    StringBuilder sb = new StringBuilder();

    try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
      stream.forEach(s -> sb.append(s).append("\n"));
    } catch (IOException e) {
      LOG.error("Exception while reading SQL file {}", filePath);
      throw new RuntimeException(e);
    }

    LOG.debug("Finished reading SQL file {}", filePath);
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
      return getOrEmpty(country.split(COUNTRY_DELIMITER, -1), 1);
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
          LOG.error("Wrong coordinate letter: {}", matcher.group(2));
          return StringUtils.EMPTY;
        }
      } else {
        LOG.error("Coordinates {} do not match pattern", location);
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
          LOG.error("Wrong coordinate letter: {}", matcher.group(4));
          return StringUtils.EMPTY;
        }
      } else {
        LOG.error("Coordinates {} do not match pattern", location);
      }
    }
    return StringUtils.EMPTY;
  }

  private String getAccession(ResultSet rs) throws SQLException {
    return rs.getString(ACCESSION_RS_INDEX);
  }

  private String getSampleAccession(ResultSet rs) throws SQLException {
    return rs.getString(SAMPLE_ACCESSION_RS_INDEX);
  }

  private String getLocation(ResultSet rs) throws SQLException {
    return rs.getString(LOCATION_RS_INDEX);
  }

  private String getCountry(ResultSet rs) throws SQLException {
    return rs.getString(COUNTRY_RS_INDEX);
  }

  private String getIdentifiedBy(ResultSet rs) throws SQLException {
    return rs.getString(IDENTIFIED_BY_RS_INDEX);
  }

  private String getCollectedBy(ResultSet rs) throws SQLException {
    return rs.getString(COLLECTED_BY_RS_INDEX);
  }

  private String getCollectionDate(ResultSet rs) throws SQLException {
    return rs.getString(COLLECTION_DATE_RS_INDEX);
  }

  private String getSpecimenVoucher(ResultSet rs) throws SQLException {
    return rs.getString(SPECIMEN_VOUCHER_RS_INDEX);
  }

  private String getSequenceMd5(ResultSet rs) throws SQLException {
    return rs.getString(SEQUENCE_MD5_RS_INDEX);
  }

  private String getScientificName(ResultSet rs) throws SQLException {
    return rs.getString(SCIENTIFIC_NAME_RS_INDEX);
  }

  private String getTaxId(ResultSet rs) throws SQLException {
    return rs.getString(TAX_ID_RS_INDEX);
  }

  private String getAltitude(ResultSet rs) throws SQLException {
    return rs.getString(ALTITUDE_RS_INDEX);
  }

  private String getSex(ResultSet rs) throws SQLException {
    return rs.getString(SEX_RS_INDEX);
  }

  private String getDescription(ResultSet rs) throws SQLException {
    return rs.getString(DESCRIPTION_RS_INDEX);
  }

  private String getHost(ResultSet rs) throws SQLException {
    return rs.getString(HOST_RS_INDEX);
  }

  private String getKingdom(ResultSet rs) throws SQLException {
    return rs.getString(KINGDOM_RS_INDEX);
  }

  private String getPhylum(ResultSet rs) throws SQLException {
    return rs.getString(PHYLUM_RS_INDEX);
  }

  private String getClass(ResultSet rs) throws SQLException {
    return rs.getString(CLASS_RS_INDEX);
  }

  private String getOrder(ResultSet rs) throws SQLException {
    return rs.getString(ORDER_RS_INDEX);
  }

  private String getFamily(ResultSet rs) throws SQLException {
    return rs.getString(FAMILY_RS_INDEX);
  }

  private String getGenus(ResultSet rs) throws SQLException {
    return rs.getString(GENUS_RS_INDEX);
  }

  private void setOccurrenceId(PreparedStatement ps, String data) throws SQLException {
    ps.setString(OCCURRENCE_ID_INDEX, data);
  }

  private void setAssociatedSequences(PreparedStatement ps, String data) throws SQLException {
    ps.setString(ASSOCIATED_SEQUENCES_INDEX, data);
  }

  private void setReferences(PreparedStatement ps, String data) throws SQLException {
    ps.setString(REFERENCES_INDEX, data);
  }

  private void setDecimalLatitude(PreparedStatement ps, String data) throws SQLException {
    ps.setString(DECIMAL_LATITUDE_INDEX, data);
  }

  private void setDecimalLongitude(PreparedStatement ps, String data) throws SQLException {
    ps.setString(DECIMAL_LONGITUDE_INDEX, data);
  }

  private void setCountry(PreparedStatement ps, String data) throws SQLException {
    ps.setString(COUNTRY_PROCESSED_INDEX, data);
  }

  private void setLocality(PreparedStatement ps, String data) throws SQLException {
    ps.setString(LOCALITY_INDEX, data);
  }

  private void setIdentifiedBy(PreparedStatement ps, String data) throws SQLException {
    ps.setString(IDENTIFIED_BY_PROCESSED_INDEX, data);
  }

  private void setRecordedBy(PreparedStatement ps, String data) throws SQLException {
    ps.setString(RECORDED_BY_INDEX, data);
  }

  private void setEventDate(PreparedStatement ps, String data) throws SQLException {
    ps.setString(EVENT_DATE_INDEX, data);
  }

  private void setCatalogNumber(PreparedStatement ps, String data) throws SQLException {
    ps.setString(CATALOG_NUMBER_INDEX, data);
  }

  private void setBasisOfRecord(PreparedStatement ps, String data) throws SQLException {
    ps.setString(BASIS_OF_RECORD_INDEX, data);
  }

  private void setTaxonId(PreparedStatement ps, String data) throws SQLException {
    ps.setString(TAXON_ID_INDEX, data);
  }

  private void setScientificName(PreparedStatement ps, String data) throws SQLException {
    ps.setString(SCIENTIFIC_NAME_PROCESSED_INDEX, data);
  }

  private void setTaxonConceptId(PreparedStatement ps, String data) throws SQLException {
    ps.setString(TAXON_CONCEPT_INDEX, data);
  }

  private void setMinimumElevation(PreparedStatement ps, String data) throws SQLException {
    ps.setString(MINIMUM_ELEVATION_IN_METERS_INDEX, data);
  }

  private void setMaximumElevation(PreparedStatement ps, String data) throws SQLException {
    ps.setString(MAXIMUM_ELEVATION_IN_METERS_INDEX, data);
  }

  private void setSex(PreparedStatement ps, String data) throws SQLException {
    ps.setString(SEX_PROCESSED_INDEX, data);
  }

  private void setOccurrenceRemarks(PreparedStatement ps, String data) throws SQLException {
    ps.setString(OCCURRENCE_REMARK_INDEX, data);
  }

  private void setAssociatedTaxa(PreparedStatement ps, String data) throws SQLException {
    ps.setString(ASSOCIATED_TAXA_INDEX, data);
  }

  private void setKingdom(PreparedStatement ps, String data) throws SQLException {
    ps.setString(KINGDOM_INDEX, data);
  }

  private void setPhylum(PreparedStatement ps, String data) throws SQLException {
    ps.setString(PHYLUM_INDEX, data);
  }

  private void setClass(PreparedStatement ps, String data) throws SQLException {
    ps.setString(CLASS_INDEX, data);
  }

  private void setOrder(PreparedStatement ps, String data) throws SQLException {
    ps.setString(ORDER_INDEX, data);
  }

  private void setFamily(PreparedStatement ps, String data) throws SQLException {
    ps.setString(FAMILY_INDEX, data);
  }

  private void setGenus(PreparedStatement ps, String data) throws SQLException {
    ps.setString(GENUS_INDEX, data);
  }
}
