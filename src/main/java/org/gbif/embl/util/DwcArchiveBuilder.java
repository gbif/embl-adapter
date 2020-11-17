package org.gbif.embl.util;

import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.terms.Term;
import org.gbif.embl.api.EmblResponse;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.gbif.embl.util.EmblAdapterConstants.ASSOCIATED_SEQUENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.COUNTRY_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.DEFAULT_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.LOCATION_PATTERN;
import static org.gbif.embl.util.EmblAdapterConstants.MATERIAL_SAMPLE;
import static org.gbif.embl.util.EmblAdapterConstants.PRESERVED_SPECIMEN;
import static org.gbif.embl.util.EmblAdapterConstants.REFERENCES_URL;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_CONCEPT_ID_URL;
import static org.gbif.embl.util.EmblAdapterConstants.TAXON_ID_PREFIX;

public class DwcArchiveBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DwcArchiveBuilder.class);

  private final long prefix = new Date().getTime();
  // TODO: 11/11/2020 use proper dir
  private final File archiveDir = new File("archives/temp" + prefix);

  public void buildArchive(File zipFile, List<EmblResponse> emblResponseList) {
    LOG.info("Start building the archive {} ", zipFile.getPath());

    try {
      if (zipFile.exists()) {
        zipFile.delete();
      }

      // metadata about the entire archive data
      generateMetadata();
      // meta.xml
      DwcArchiveUtils.createArchiveDescriptor(archiveDir);
      // occurrence.txt
      createCoreFile(emblResponseList);
      // zip up
      LOG.info("Zipping archive {}", archiveDir);
      CompressionUtil.zipDir(archiveDir, zipFile, true);
    } catch (IOException e) {
      LOG.error("Error while building archive", e);
    } finally {
      // always cleanUp temp dir
      cleanupFS();
    }
  }

  private void createCoreFile(List<EmblResponse> emblResponseList) throws IOException {
    File csvOutputFile = new File(archiveDir, EmblAdapterConstants.CORE_FILENAME);
    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
      pw.println(
          EmblAdapterConstants.TERMS.stream()
              .map(Term::simpleName)
              .collect(Collectors.joining(DEFAULT_DELIMITER))
      );

      emblResponseList
          .stream()
          .map(this::joinDataTogether)
          .forEach(pw::println);
    }
  }

  private String joinDataTogether(EmblResponse data) {
    return String.join(DEFAULT_DELIMITER,
        trimToEmpty(data.getAccession()),
        toAssociatedSequences(data.getAccession()),
        toReferences(data.getAccession()),
        toLatitude(data.getLocation()),
        toLongitude(data.getLocation()),
        toCountry(data.getCountry()),
        toLocality(data.getCountry()),
        trimToEmpty(data.getIdentifiedBy()),
        trimToEmpty(data.getCollectedBy()),
        trimToEmpty(data.getCollectionDate()),
        trimToEmpty(data.getSpecimenVoucher()),
        toBasisOfRecord(data.getSpecimenVoucher()),
        toTaxonId(data.getSequenceMd5()),
        trimToEmpty(data.getScientificName()),
        toTaxonConceptId(data.getTaxId()),
        trimToEmpty(data.getAltitude()),
        trimToEmpty(data.getAltitude()),
        trimToEmpty(data.getSex())
        );
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
      return country.split(COUNTRY_DELIMITER)[1];
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
    File source = new File("src/main/resources/eml.xml");
    if (!archiveDir.exists()) {
      archiveDir.mkdir();
    }
    File target = new File(archiveDir, "eml.xml");
    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  private void cleanupFS() {
    LOG.info("Cleaning up archive directory {}", archiveDir.getPath());
    if (archiveDir.exists()) {
      FileUtils.deleteDirectoryRecursively(archiveDir);
    }
  }
}
