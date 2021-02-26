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

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class EmblAdapterConstants {

  public static final int DEFAULT_START_HOUR = 0;
  public static final int DEFAULT_START_MINUTE = 0;
  public static final int DEFAULT_FREQUENCY = 7;

  public static final int BATCH_SIZE = 5000;

  public static final String METADATA_FILENAME = "metadata.xml";
  public static final String CORE_FILENAME = "occurrence.txt";
  public static final String DESCRIPTOR_FILENAME = "meta.xml";

  public static final DateTimeFormatter DATE_NO_SEPARATORS_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  public static final Pattern LOCATION_PATTERN =
      Pattern.compile("([0-9.]+\\s+\\w)\\s+([0-9.]+\\s+\\w)");

  public static final String DEFAULT_DELIMITER = "\t";
  public static final String COUNTRY_DELIMITER = ":";
  public static final String TAXON_ID_PREFIX = "ASV:";
  public static final String ASSOCIATED_SEQUENCES_URL =
      "https://www.ebi.ac.uk/ena/browser/api/embl/";
  public static final String REFERENCES_URL = "https://www.ebi.ac.uk/ena/browser/view/";
  public static final String TAXON_CONCEPT_ID_URL = "https://www.ebi.ac.uk/ena/browser/view/Taxon:";

  public static final String PRESERVED_SPECIMEN = "PreservedSpecimen";
  public static final String MATERIAL_SAMPLE = "MaterialSample";

  public static final String TAXONOMY_TABLE = "ena_taxonomy";
  public static final String SQL_CLEAN = "TRUNCATE embl_data";
  public static final String SQL_CLEAN_TAXONOMY = "TRUNCATE ena_taxonomy";
  public static final String SQL_INSERT =
      "INSERT INTO embl_data(accession, location, country, "
          + "identified_by, collected_by, collection_date, specimen_voucher, sequence_md5, scientific_name, "
          + "tax_id, altitude, sex, description) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
  public static final String SQL_INSERT_TAXONOMY =
      "INSERT INTO ena_taxonomy(taxon_id, kingdom, phylum, "
          + "class, \"order\", family, genus) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

  public static final List<Term> TERMS =
      Arrays.asList(
          DwcTerm.occurrenceID,
          DwcTerm.associatedSequences,
          DcTerm.references,
          DwcTerm.decimalLatitude,
          DwcTerm.decimalLongitude,
          DwcTerm.country,
          DwcTerm.locality,
          DwcTerm.identifiedBy,
          DwcTerm.recordedBy,
          DwcTerm.eventDate,
          DwcTerm.catalogNumber,
          DwcTerm.basisOfRecord,
          DwcTerm.taxonID,
          DwcTerm.scientificName,
          DwcTerm.taxonConceptID,
          DwcTerm.minimumElevationInMeters,
          DwcTerm.maximumElevationInMeters,
          DwcTerm.sex,
          DwcTerm.occurrenceRemarks,
          DwcTerm.kingdom,
          DwcTerm.phylum,
          DwcTerm.class_,
          DwcTerm.order,
          DwcTerm.family,
          DwcTerm.genus);
  public static final int ACCESSION_INDEX = 0;
  public static final int LOCATION_INDEX = 1;
  public static final int COUNTRY_INDEX = 2;
  public static final int IDENTIFIED_BY_INDEX = 3;
  public static final int COLLECTED_BY_INDEX = 4;
  public static final int COLLECTION_DATE_INDEX = 5;
  public static final int SPECIMEN_VOUCHER_INDEX = 6;
  public static final int SEQUENCE_MD5_INDEX = 7;
  public static final int SCIENTIFIC_NAME_INDEX = 8;
  public static final int TAX_ID_INDEX = 9;
  public static final int ALTITUDE_INDEX = 10;
  public static final int SEX_INDEX = 11;
  public static final int DESCRIPTION_INDEX = 12;
  public static final int ACCESSION_RS_INDEX = 1;
  public static final int LOCATION_RS_INDEX = 2;
  public static final int COUNTRY_RS_INDEX = 3;
  public static final int IDENTIFIED_BY_RS_INDEX = 4;
  public static final int COLLECTED_BY_RS_INDEX = 5;
  public static final int COLLECTION_DATE_RS_INDEX = 6;
  public static final int SPECIMEN_VOUCHER_RS_INDEX = 7;
  public static final int SEQUENCE_MD5_RS_INDEX = 8;
  public static final int SCIENTIFIC_NAME_RS_INDEX = 9;
  public static final int TAX_ID_RS_INDEX = 10;
  public static final int ALTITUDE_RS_INDEX = 11;
  public static final int SEX_RS_INDEX = 12;
  public static final int DESCRIPTION_RS_INDEX = 13;
  public static final int KINGDOM_RS_INDEX = 14;
  public static final int PHYLUM_RS_INDEX = 15;
  public static final int CLASS_RS_INDEX = 16;
  public static final int ORDER_RS_INDEX = 17;
  public static final int FAMILY_RS_INDEX = 18;
  public static final int GENUS_RS_INDEX = 19;
  public static final int TAXON_ID_SELECT_INDEX = 1;
  public static final int KINGDOM_SELECT_INDEX = 2;
  public static final int PHYLUM_SELECT_INDEX = 3;
  public static final int CLASS_SELECT_INDEX = 4;
  public static final int ORDER_SELECT_INDEX = 5;
  public static final int FAMILY_SELECT_INDEX = 6;
  public static final int GENUS_SELECT_INDEX = 7;

  private EmblAdapterConstants() {}
}
