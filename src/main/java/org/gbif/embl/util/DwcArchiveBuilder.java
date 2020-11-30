package org.gbif.embl.util;

import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
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

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DwcArchiveBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DwcArchiveBuilder.class);

  private final String workingDirectory;
  private final String metadataFilePath;
  private final File archiveDir;

  public DwcArchiveBuilder(String workingDirectory, String metadataFilePath) {
    this.workingDirectory = workingDirectory;
    this.metadataFilePath = metadataFilePath;
    this.archiveDir = new File(workingDirectory + "/temp" + new Date().getTime());
  }

  public void buildArchive(File zipFile, ResultSet rs) {
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
      createCoreFile(rs);
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

  private void createCoreFile(ResultSet resultSet) throws IOException {
    LOG.debug("Creating core file {} in {}", EmblAdapterConstants.CORE_FILENAME, archiveDir);
    File csvOutputFile = new File(archiveDir, EmblAdapterConstants.CORE_FILENAME);
    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
      pw.println(
          EmblAdapterConstants.TERMS.stream()
              .map(Term::simpleName)
              .collect(Collectors.joining(DEFAULT_DELIMITER))
      );

      while (resultSet.next()) {
        pw.println(joinData(resultSet));
      }
    } catch (SQLException e) {
      LOG.error("SQL exception while creating core file", e);
    }
  }

  private String joinData(ResultSet rs) throws SQLException {
    return String.join(DEFAULT_DELIMITER,
        trimToEmpty(rs.getString("accession")),
        toAssociatedSequences(rs.getString("accession")),
        toReferences(rs.getString("accession")),
        toLatitude(rs.getString("location")),
        toLongitude(rs.getString("location")),
        toCountry(rs.getString("country")),
        toLocality(rs.getString("country")),
        trimToEmpty(rs.getString("identified_by")),
        trimToEmpty(rs.getString("collected_by")),
        trimToEmpty(rs.getString("collection_date")),
        trimToEmpty(rs.getString("specimen_voucher")),
        toBasisOfRecord(rs.getString("specimen_voucher")),
        toTaxonId(rs.getString("sequence_md5")),
        trimToEmpty(rs.getString("scientific_name")),
        toTaxonConceptId(rs.getString("tax_id")),
        trimToEmpty(rs.getString("altitude")),
        trimToEmpty(rs.getString("altitude")),
        trimToEmpty(rs.getString("sex"))
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
