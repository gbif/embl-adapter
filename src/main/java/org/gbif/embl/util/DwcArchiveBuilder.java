package org.gbif.embl.util;

import org.gbif.dwc.terms.DwcTerm;
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
      // TODO: 11/11/2020 process exception somehow
      e.printStackTrace();
    } finally {
      // always cleanUp temp dir
      cleanupFS();
    }
  }

  private void createCoreFile(List<EmblResponse> emblResponseList) throws IOException {
    File csvOutputFile = new File(archiveDir, EmblAdapterConstants.CORE_FILENAME);
    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
      pw.println(String.join(";", DwcTerm.occurrenceID.simpleName()));

      emblResponseList
          .stream()
          .map(this::joinDataTogether)
          .forEach(pw::println);
    }
  }

  private String joinDataTogether(EmblResponse data) {
    return String.join(";", data.getAccession());
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
