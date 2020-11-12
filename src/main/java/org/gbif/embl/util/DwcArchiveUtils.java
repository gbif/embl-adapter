package org.gbif.embl.util;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveField;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.MetaDescriptorWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.embl.util.EmblAdapterConstants.CORE_FILENAME;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTOR_FILENAME;
import static org.gbif.embl.util.EmblAdapterConstants.METADATA_FILENAME;

/**
 * Utility class for Darwin Core Archive handling during the file creation.
 */
public class DwcArchiveUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DwcArchiveUtils.class);

  private static final String DEFAULT_DELIMITER = ";";

  /**
   * Hidden constructor.
   */
  private DwcArchiveUtils() {
    // private empty constructor
  }

  /**
   * Creates a new archive file description for a DwC archive and sets the id field to the column of gbifID.
   * Used to generate the meta.xml with the help of the dwca-writer
   */
  public static ArchiveFile createArchiveFile(String filename, Term rowType, Iterable<? extends Term> columns) {
    return createArchiveFile(filename, rowType, columns, Collections.emptyMap());
  }

  /**
   * Creates a new archive file description for a DwC archive and sets the id field to the column of gbifID.
   * Used to generate the meta.xml with the help of the dwca-writer
   */
  public static ArchiveFile createArchiveFile(String filename, Term rowType, Iterable<? extends Term> columns,
                                              Map<? extends Term,String> defaultColumns) {
    ArchiveFile af = buildBaseArchive(filename, rowType);
    int index = 0;
    for (Term term : columns) {
      ArchiveField field = new ArchiveField();
      field.setIndex(index);
      field.setTerm(term);
      field.setDelimitedBy(DEFAULT_DELIMITER);
      af.addField(field);
      index++;
    }
    for (Map.Entry<? extends Term,String> defaultTerm : defaultColumns.entrySet()) {
      ArchiveField defaultField = new ArchiveField();
      defaultField.setTerm(defaultTerm.getKey());
      defaultField.setDefaultValue(defaultTerm.getValue());
      af.addField(defaultField);
    }
    ArchiveField coreId = af.getField(GbifTerm.gbifID);
    // TODO: 12/11/2020 remove gbifID?
    if (coreId == null) {
      throw new IllegalArgumentException("Archive columns MUST include the gbif:gbifID term");
    }
    af.setId(coreId);
    return af;
  }

  /**
   * Utility function that creates an archive with common/default settings.
   */
  private static ArchiveFile buildBaseArchive(String filename, Term rowType) {
    ArchiveFile af = new ArchiveFile();
    af.addLocation(filename);
    af.setRowType(rowType);
    af.setEncoding(Charsets.UTF_8.displayName());
    af.setIgnoreHeaderLines(1);
    af.setFieldsEnclosedBy(null);
    af.setFieldsTerminatedBy("\t");
    af.setLinesTerminatedBy("\n");
    return af;
  }

  /**
   * Creates an meta.xml descriptor file in the directory parameter.
   */
  public static void createArchiveDescriptor(File directory) {
    LOG.info("Creating archive meta.xml descriptor");

    Archive downloadArchive = new Archive();
    downloadArchive.setMetadataLocation(METADATA_FILENAME);

    // TODO: 11/11/2020 DwcTerm.Occurrence?
    // TODO: 11/11/2020 set all columns
    // TODO: 11/11/2020 default columns?
    ArchiveFile coreFile = createArchiveFile(
        CORE_FILENAME,
        DwcTerm.Occurrence,
        Arrays.asList(GbifTerm.gbifID, DwcTerm.occurrenceID)
        );
    downloadArchive.setCore(coreFile);

    try {
      File metaFile = new File(directory, DESCRIPTOR_FILENAME);
      MetaDescriptorWriter.writeMetaFile(metaFile, downloadArchive);
    } catch (IOException e) {
      LOG.error("Error creating meta.xml file", e);
      throw new RuntimeException(e);
    }
  }
}
