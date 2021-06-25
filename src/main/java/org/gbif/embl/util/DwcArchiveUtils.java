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

import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveField;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.MetaDescriptorWriter;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import static org.gbif.embl.util.EmblAdapterConstants.CORE_FILENAME;
import static org.gbif.embl.util.EmblAdapterConstants.DEFAULT_DELIMITER;
import static org.gbif.embl.util.EmblAdapterConstants.DESCRIPTOR_FILENAME;
import static org.gbif.embl.util.EmblAdapterConstants.METADATA_FILENAME;

/**
 * Utility class for Darwin Core Archive handling during the file creation.
 */
public final class DwcArchiveUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DwcArchiveUtils.class);

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
  public static ArchiveFile createArchiveFile(
      String filename, Term rowType, Iterable<? extends Term> columns) {
    return createArchiveFile(filename, rowType, columns, Collections.emptyMap());
  }

  /**
   * Creates a new archive file description for a DwC archive and sets the id field to the column of gbifID.
   * Used to generate the meta.xml with the help of the dwca-writer
   */
  public static ArchiveFile createArchiveFile(
      String filename,
      Term rowType,
      Iterable<? extends Term> columns,
      Map<? extends Term, String> defaultColumns) {
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
    for (Map.Entry<? extends Term, String> defaultTerm : defaultColumns.entrySet()) {
      ArchiveField defaultField = new ArchiveField();
      defaultField.setTerm(defaultTerm.getKey());
      defaultField.setDefaultValue(defaultTerm.getValue());
      af.addField(defaultField);
    }
    ArchiveField coreId = af.getField(DwcTerm.occurrenceID);
    if (coreId == null) {
      throw new IllegalArgumentException("Archive columns MUST include the dwc:occurrenceID term");
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
  public static void createArchiveDescriptor(File directory, List<Term> terms) {
    LOG.info("Creating archive descriptor file meta.xml in {}", directory);
    Archive downloadArchive = new Archive();
    downloadArchive.setMetadataLocation(METADATA_FILENAME);

    ArchiveFile coreFile =
        createArchiveFile(CORE_FILENAME, DwcTerm.Occurrence, terms);
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
