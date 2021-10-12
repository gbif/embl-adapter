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
package org.gbif.embl.util;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.embl.util.EmblAdapterConstants.ACCESSION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ALTITUDE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ASSOCIATED_SEQUENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.BATCH_SIZE;
import static org.gbif.embl.util.EmblAdapterConstants.CLASS_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTION_DATE_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DATE_FORMAT;
import static org.gbif.embl.util.EmblAdapterConstants.DEFAULT_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.FAMILY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.GENUS_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.HOST_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.KINGDOM_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_PATTERN;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.MATERIAL_SAMPLE;
import static org.gbif.embl.util.EmblAdapterConstants.ORDER_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.PHYLUM_RS_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.PRESERVED_SPECIMEN;
import static org.gbif.embl.util.EmblAdapterConstants.REFERENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.SAMPLE_ACCESSION_RS_INDEX;
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

  public DwcArchiveBuilder(DataSource dataSource, String workingDirectory) {
    this.dataSource = dataSource;
    this.workingDirectory = workingDirectory;
  }

  public void buildArchive(
      File zipFile, String tableName, String query, String metadataFilePath, List<Term> terms) {
    LOG.info("Start building the archive {} ", zipFile.getPath());
    File archiveDir = new File(workingDirectory + "/temp_" + UUID.randomUUID());

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
      generateMetadata(archiveDir, metadataFilePath);
      // meta.xml
      DwcArchiveUtils.createArchiveDescriptor(archiveDir, terms);
      // occurrence.txt
      createCoreFile(archiveDir, tableName, query, terms);
      // zip up
      LOG.info("Zipping archive {}", archiveDir);
      CompressionUtil.zipDir(archiveDir, zipFile, true);
    } catch (IOException | SQLException e) {
      LOG.error("Error while building archive", e);
      throw new RuntimeException(e);
    } finally {
      // always clean temp dir
      cleanTempDir(archiveDir);
    }
  }

  private void createCoreFile(File archiveDir, String tableName, String query, List<Term> terms)
      throws IOException, SQLException {
    LOG.debug("Creating core file {} in {}", EmblAdapterConstants.CORE_FILENAME, archiveDir);
    File outputFile = new File(archiveDir, EmblAdapterConstants.CORE_FILENAME);

    Set<String> recordsSeenBefore = new HashSet<>();

    // SQL select for table
    String sqlSelect = readSqlFile(query).replace("embl_data", tableName).trim();
    LOG.debug("SQL select: {}", sqlSelect);

    // write core file
    try (Connection connection = dataSource.getConnection();
        PrintWriter pw = new PrintWriter(outputFile)) {
      LOG.debug("DB connection established to retrieve core file data");
      connection.setAutoCommit(false);

      // file header
      pw.println(
          terms.stream().map(Term::simpleName).collect(Collectors.joining(DEFAULT_DELIMITER)));
      LOG.debug("Core file header");

      try (Statement statement = connection.createStatement()) {
        statement.setFetchSize(BATCH_SIZE);

        try (ResultSet rs = statement.executeQuery(sqlSelect)) {
          LOG.debug("Start writing core file data");
          // file data
          while (rs.next()) {
            // skip records with missing specimen_voucher and collection_date
            if (StringUtils.isEmpty(rs.getString(SPECIMEN_VOUCHER_RS_INDEX))
                && StringUtils.isEmpty(rs.getString(COLLECTION_DATE_RS_INDEX))) {
              continue;
            }

            // check if the record was seen before
            if (StringUtils.isNotEmpty(rs.getString(SAMPLE_ACCESSION_RS_INDEX))
                && StringUtils.isNotEmpty(rs.getString(SCIENTIFIC_NAME_RS_INDEX))) {
              String sampleAccessionPlusScientificName =
                  rs.getString(SAMPLE_ACCESSION_RS_INDEX) + rs.getString(SCIENTIFIC_NAME_RS_INDEX);

              // skip duplicate records (seen before) based on sample_accession and scientific_name
              // otherwise remember and write it
              if (recordsSeenBefore.contains(sampleAccessionPlusScientificName)) {
                continue;
              } else {
                recordsSeenBefore.add(sampleAccessionPlusScientificName);
              }
            }

            pw.println(
                joinData(
                    rs,
                    terms.contains(DwcTerm.associatedTaxa)
                        ? Collections.emptyList()
                        : Collections.singletonList(HOST_RS_INDEX)));
          }
        }
      }
      LOG.debug("Finished writing core file");
    }
  }

  private String joinData(ResultSet rs, List<Integer> skipPositions) throws SQLException {
    StringJoiner joiner = new StringJoiner(DEFAULT_DELIMITER);

    joinItem(
        joiner,
        rs,
        ACCESSION_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // occurrenceID term
    joinItem(
        joiner,
        rs,
        ACCESSION_RS_INDEX,
        this::toAssociatedSequences,
        skipPositions); // associatedSequences term
    joinItem(joiner, rs, ACCESSION_RS_INDEX, this::toReferences, skipPositions); // references term
    joinItem(
        joiner, rs, LOCATION_RS_INDEX, this::toLatitude, skipPositions); // decimalLatitude term
    joinItem(
        joiner, rs, LOCATION_RS_INDEX, this::toLongitude, skipPositions); // decimalLongitude term
    joinItem(joiner, rs, COUNTRY_RS_INDEX, this::toCountry, skipPositions); // country term
    joinItem(joiner, rs, COUNTRY_RS_INDEX, this::toLocality, skipPositions); // locality term
    joinItem(
        joiner,
        rs,
        IDENTIFIED_BY_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // identifiedBy term
    joinItem(
        joiner,
        rs,
        COLLECTED_BY_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // recordedBy term
    joinItem(
        joiner,
        rs,
        COLLECTION_DATE_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // eventDate term
    joinItem(
        joiner,
        rs,
        SPECIMEN_VOUCHER_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // catalogNumber term
    joinItem(
        joiner,
        rs,
        SPECIMEN_VOUCHER_RS_INDEX,
        this::toBasisOfRecord,
        skipPositions); // basisOfRecord term
    joinItem(joiner, rs, SEQUENCE_MD5_RS_INDEX, this::toTaxonId, skipPositions); // taxonID term
    joinItem(
        joiner,
        rs,
        SCIENTIFIC_NAME_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // scientificName term
    joinItem(
        joiner, rs, TAX_ID_RS_INDEX, this::toTaxonConceptId, skipPositions); // taxonConceptID term
    joinItem(
        joiner,
        rs,
        ALTITUDE_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // minimumElevationInMeters term
    joinItem(
        joiner,
        rs,
        ALTITUDE_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // maximumElevationInMeters term
    joinItem(joiner, rs, SEX_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // sex term
    joinItem(
        joiner,
        rs,
        DESCRIPTION_RS_INDEX,
        StringUtils::trimToEmpty,
        skipPositions); // occurrenceRemarks term
    joinItem(
        joiner, rs, HOST_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // associatedTaxa term
    joinItem(joiner, rs, KINGDOM_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // kingdom term
    joinItem(joiner, rs, PHYLUM_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // phylum term
    joinItem(joiner, rs, CLASS_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // class term
    joinItem(joiner, rs, ORDER_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // order term
    joinItem(joiner, rs, FAMILY_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // family term
    joinItem(joiner, rs, GENUS_RS_INDEX, StringUtils::trimToEmpty, skipPositions); // genus term

    return joiner.toString();
  }

  private void joinItem(
      StringJoiner sj,
      ResultSet rs,
      Integer position,
      Function<String, String> processor,
      List<Integer> skipPositions)
      throws SQLException {
    if (!skipPositions.contains(position)) {
      sj.add(processor.apply(rs.getString(position)));
    }
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
        return matcher.group(1);
      }
    }
    return StringUtils.EMPTY;
  }

  private String toLongitude(String location) {
    if (StringUtils.isNotBlank(location)) {
      Matcher matcher = LOCATION_PATTERN.matcher(location);
      if (matcher.find()) {
        return matcher.group(2);
      }
    }
    return StringUtils.EMPTY;
  }

  private void generateMetadata(File archiveDir, String metadataFilePath) throws IOException {
    LOG.debug("Creating metadata file eml.xml in {}", archiveDir);
    File file = new File(metadataFilePath);

    if (file.exists()) {
      LOG.debug("Metadata file is present, inserting actual pubDate");

      // replacing pubDate with actual date
      String fileContent =
          org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      fileContent = fileContent.replace("${pubDate}", LocalDate.now().format(DATE_FORMAT));

      // write data to eml.xml
      File target = new File(archiveDir, "eml.xml");
      org.apache.commons.io.FileUtils.writeStringToFile(
          target, fileContent, StandardCharsets.UTF_8);
    } else {
      LOG.error("Metadata file eml.xml is not present in {}", workingDirectory);
    }
  }

  private void cleanTempDir(File archiveDir) {
    LOG.debug("Cleaning up archive directory {}", archiveDir.getPath());
    if (archiveDir.exists()) {
      FileUtils.deleteDirectoryRecursively(archiveDir);
    }
  }
}
