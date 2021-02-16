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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.gbif.embl.util.EmblAdapterConstants.ACCESSION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ALTITUDE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.ASSOCIATED_SEQUENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTED_BY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COLLECTION_DATE_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.DEFAULT_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.IDENTIFIED_BY_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_PATTERN;
import static org.gbif.embl.util.EmblAdapterConstants.MATERIAL_SAMPLE;
import static org.gbif.embl.util.EmblAdapterConstants.PRESERVED_SPECIMEN;
import static org.gbif.embl.util.EmblAdapterConstants.REFERENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.SCIENTIFIC_NAME_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEQUENCE_MD5_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SEX_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.SPECIMEN_VOUCHER_INDEX;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_CONCEPT_ID_URL;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_ID_PREFIX;
import static org.gbif.embl.util.EmblAdapterConstants.TAX_ID_INDEX;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DwcArchiveBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DwcArchiveBuilder.class);

  private final String workingDirectory;
  private final String metadataFilePath;
  private final File archiveDir;

  public DwcArchiveBuilder(String workingDirectory, String metadataFilePath) {
    this.workingDirectory = workingDirectory;
    this.metadataFilePath = metadataFilePath;
    this.archiveDir = new File(workingDirectory + "/temp_" + UUID.randomUUID());
  }

  public void buildArchive(File zipFile, String rawDataFile) {
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
      generateMetadata();
      // meta.xml
      DwcArchiveUtils.createArchiveDescriptor(archiveDir);
      // occurrence.txt
      createCoreFile(rawDataFile);
      // zip up
      LOG.info("Zipping archive {}", archiveDir);
      CompressionUtil.zipDir(archiveDir, zipFile, true);
    } catch (IOException e) {
      LOG.error("Error while building archive", e);
    } finally {
      // always clean temp dir
      cleanTempDir();
    }
  }

  private void createCoreFile(String rawDataFile) throws IOException {
    LOG.debug("Creating core file {} in {}", EmblAdapterConstants.CORE_FILENAME, archiveDir);
    File outputFile = new File(archiveDir, EmblAdapterConstants.CORE_FILENAME);
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      pw.println(
          EmblAdapterConstants.TERMS.stream()
              .map(Term::simpleName)
              .collect(Collectors.joining(DEFAULT_DELIMITER)));

      File inputFile = new File(rawDataFile);

      try (InputStream inputStream = new FileInputStream(inputFile);
          BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
        br.lines()
            .skip(1)
            .map(line -> line.split("\\t", -1))
            .map(this::joinData)
            .forEach(pw::println);
      } catch (IOException e) {
        LOG.error("IOException while writing core file", e);
      }
    }
  }

  private String joinData(String[] arr) {
    return String.join(
        DEFAULT_DELIMITER,
        trimToEmpty(arr[ACCESSION_INDEX]), // occurrenceID term
        toAssociatedSequences(arr[ACCESSION_INDEX]), // associatedSequences term
        toReferences(arr[ACCESSION_INDEX]), // references term
        toLatitude(arr[LOCATION_INDEX]), // decimalLatitude term
        toLongitude(arr[LOCATION_INDEX]), // decimalLongitude term
        toCountry(arr[COUNTRY_INDEX]), // country term
        toLocality(arr[COUNTRY_INDEX]), // locality term
        trimToEmpty(arr[IDENTIFIED_BY_INDEX]), // identifiedBy term
        trimToEmpty(arr[COLLECTED_BY_INDEX]), // recordedBy term
        trimToEmpty(arr[COLLECTION_DATE_INDEX]), // eventDate term
        trimToEmpty(arr[SPECIMEN_VOUCHER_INDEX]), // catalogNumber term
        toBasisOfRecord(arr[SPECIMEN_VOUCHER_INDEX]), // basisOfRecord term
        toTaxonId(arr[SEQUENCE_MD5_INDEX]), // taxonID term
        trimToEmpty(arr[SCIENTIFIC_NAME_INDEX]), // scientificName term
        toTaxonConceptId(getOrEmpty(arr, TAX_ID_INDEX)), // taxonConceptID term
        trimToEmpty(getOrEmpty(arr, ALTITUDE_INDEX)), // minimumElevationInMeters term
        trimToEmpty(getOrEmpty(arr, ALTITUDE_INDEX)), // maximumElevationInMeters term
        trimToEmpty(getOrEmpty(arr, SEX_INDEX)) // sex term
        );
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

  private void generateMetadata() throws IOException {
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
