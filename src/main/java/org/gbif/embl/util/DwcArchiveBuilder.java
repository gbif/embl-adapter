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
package org.gbif.embl.util;

import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.gbif.embl.util.EmblAdapterConstants.ACCESSION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ALTITUDE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ASSOCIATED_SEQUENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.CLASS_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTION_DATE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DEFAULT_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.FAMILY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.GENUS_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.KINGDOM_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_PATTERN;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.MATERIAL_SAMPLE;
import static org.gbif.embl.util.EmblAdapterConstants.ORDER_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.PHYLUM_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.PRESERVED_SPECIMEN;
import static org.gbif.embl.util.EmblAdapterConstants.REFERENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.SCIENTIFIC_NAME_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEQUENCE_MD5_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEX_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SPECIMEN_VOUCHER_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_CONCEPT_ID_URL;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_ID_PREFIX;
import static org.gbif.embl.util.EmblAdapterConstants.TAX_ID_RS_INDEX;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DwcArchiveBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DwcArchiveBuilder.class);

  private final DataSource dataSource;
  private final String workingDirectory;
  private final File archiveDir;

  public DwcArchiveBuilder(DataSource dataSource, String workingDirectory) {
    this.dataSource = dataSource;
    this.workingDirectory = workingDirectory;
    this.archiveDir = new File(workingDirectory + "/temp_" + UUID.randomUUID());
  }

  public void buildArchive(File zipFile, String tableName, String query, String metadataFilePath) {
    LOG.info("Start building the archive {} ", zipFile.getPath());

    try {
      if (!zipFile.getParentFile().exists()) {
        LOG.debug("Directory {} does not exist, creating", zipFile.getParent());
        zipFile.getParentFile().mkdir();
      }

      if (!archiveDir.exists()) {
        LOG.debug("Directory {} does not exist, creating", archiveDir);
        archiveDir.mkdir();
      }

      if (zipFile.exists()) {
        LOG.debug("Archive file already exists, deleting");
        zipFile.delete();
      }

      // metadata about the entire archive data
      generateMetadata(metadataFilePath);
      // meta.xml
      DwcArchiveUtils.createArchiveDescriptor(archiveDir);
      // occurrence.txt
      createCoreFile(tableName, query);
      // zip up
      LOG.info("Zipping archive {}", archiveDir);
      CompressionUtil.zipDir(archiveDir, zipFile, true);
    } catch (IOException | SQLException e) {
      LOG.error("Error while building archive", e);
      throw new RuntimeException(e);
    } finally {
      // always clean temp dir
      cleanTempDir();
    }
  }

  private void createCoreFile(String tableName, String query) throws IOException, SQLException {
    LOG.debug("Creating core file {} in {}", EmblAdapterConstants.CORE_FILENAME, archiveDir);
    File outputFile = new File(archiveDir, EmblAdapterConstants.CORE_FILENAME);

    LOG.debug("Creating core file by using DB table {}", tableName);

    // SQL select for table
    String sqlSelect = readSqlFile(query).replace("embl_data", tableName);
    LOG.debug("SQL select: {}", sqlSelect);

    // write core file
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sqlSelect);
        PrintWriter pw = new PrintWriter(outputFile)) {
      // file header
      pw.println(
          EmblAdapterConstants.TERMS.stream()
              .map(Term::simpleName)
              .collect(Collectors.joining(DEFAULT_DELIMITER)));

      // file data
      while (rs.next()) {
        pw.println(joinData(rs));
      }
    }
  }

  private String joinData(ResultSet rs) throws SQLException {
    return String.join(
        DEFAULT_DELIMITER,
        trimToEmpty(rs.getString(ACCESSION_RS_INDEX)), // occurrenceID term
        toAssociatedSequences(rs.getString(ACCESSION_RS_INDEX)), // associatedSequences term
        toReferences(rs.getString(ACCESSION_RS_INDEX)), // references term
        toLatitude(rs.getString(LOCATION_RS_INDEX)), // decimalLatitude term
        toLongitude(rs.getString(LOCATION_RS_INDEX)), // decimalLongitude term
        toCountry(rs.getString(COUNTRY_RS_INDEX)), // country term
        toLocality(rs.getString(COUNTRY_RS_INDEX)), // locality term
        trimToEmpty(rs.getString(IDENTIFIED_BY_RS_INDEX)), // identifiedBy term
        trimToEmpty(rs.getString(COLLECTED_BY_RS_INDEX)), // recordedBy term
        trimToEmpty(rs.getString(COLLECTION_DATE_RS_INDEX)), // eventDate term
        trimToEmpty(rs.getString(SPECIMEN_VOUCHER_RS_INDEX)), // catalogNumber term
        toBasisOfRecord(rs.getString(SPECIMEN_VOUCHER_RS_INDEX)), // basisOfRecord term
        toTaxonId(rs.getString(SEQUENCE_MD5_RS_INDEX)), // taxonID term
        trimToEmpty(rs.getString(SCIENTIFIC_NAME_RS_INDEX)), // scientificName term
        toTaxonConceptId(rs.getString(TAX_ID_RS_INDEX)), // taxonConceptID term
        trimToEmpty(rs.getString(ALTITUDE_RS_INDEX)), // minimumElevationInMeters term
        trimToEmpty(rs.getString(ALTITUDE_RS_INDEX)), // maximumElevationInMeters term
        trimToEmpty(rs.getString(SEX_RS_INDEX)), // sex term
        trimToEmpty(rs.getString(DESCRIPTION_RS_INDEX)), // occurrenceRemarks term
        trimToEmpty(rs.getString(KINGDOM_RS_INDEX)), // kingdom term
        trimToEmpty(rs.getString(PHYLUM_RS_INDEX)), // phylum term
        trimToEmpty(rs.getString(CLASS_RS_INDEX)), // class term
        trimToEmpty(rs.getString(ORDER_RS_INDEX)), // order term
        trimToEmpty(rs.getString(FAMILY_RS_INDEX)), // family term
        trimToEmpty(rs.getString(GENUS_RS_INDEX)) // genus term
        );
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

  private String getOrEmpty(String[] arr, int index) {
    return arr.length > index ? arr[index] : StringUtils.EMPTY;
  }

  private CharSequence toTaxonConceptId(String data) {
    return StringUtils.isNotBlank(data) ? TAXON_CONCEPT_ID_URL + data : StringUtils.EMPTY;
  }

  private CharSequence toReferences(String data) {
    return StringUtils.isNotBlank(data) ? REFERENCES_URL + data : StringUtils.EMPTY;
  }

  private CharSequence toAssociatedSequences(String data) {
    return StringUtils.isNotBlank(data) ? ASSOCIATED_SEQUENCES_URL + data : StringUtils.EMPTY;
  }

  private CharSequence toTaxonId(String data) {
    return StringUtils.isNotBlank(data) ? TAXON_ID_PREFIX + data : StringUtils.EMPTY;
  }

  private CharSequence toBasisOfRecord(String data) {
    return StringUtils.isNotBlank(data) ? PRESERVED_SPECIMEN : MATERIAL_SAMPLE;
  }

  private CharSequence toCountry(String country) {
    if (StringUtils.isNotBlank(country) && country.contains(COUNTRY_DELIMITER)) {
      return country.split(COUNTRY_DELIMITER)[0];
    }

    return StringUtils.EMPTY;
  }

  private CharSequence toLocality(String country) {
    if (StringUtils.isNotBlank(country) && country.contains(COUNTRY_DELIMITER)) {
      return getOrEmpty(country.split(COUNTRY_DELIMITER, -1), 1);
    }

    return StringUtils.EMPTY;
  }

  private CharSequence toLatitude(String location) {
    if (StringUtils.isNotBlank(location)) {
      Matcher matcher = LOCATION_PATTERN.matcher(location);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    return StringUtils.EMPTY;
  }

  private CharSequence toLongitude(String location) {
    if (StringUtils.isNotBlank(location)) {
      Matcher matcher = LOCATION_PATTERN.matcher(location);
      if (matcher.find()) {
        return matcher.group(2);
      }
    }
    return StringUtils.EMPTY;
  }

  private void generateMetadata(String metadataFilePath) throws IOException {
    LOG.debug("Creating metadata file eml.xml in {}", archiveDir);
    File file = new File(metadataFilePath);

    if (file.exists()) {
      LOG.debug("Metadata file is present, copying");
      File target = new File(archiveDir, "eml.xml");
      Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } else {
      LOG.error("Metadata file eml.xml is not present in {}", workingDirectory);
    }
  }

  private void cleanTempDir() {
    LOG.debug("Cleaning up archive directory {}", archiveDir.getPath());
    if (archiveDir.exists()) {
      FileUtils.deleteDirectoryRecursively(archiveDir);
    }
  }
}
