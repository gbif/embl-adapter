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

  public static final String SQL_CLEAN = "TRUNCATE embl_data";
  public static final String SQL_INSERT =
      "INSERT INTO embl_data(accession, location, country, "
          + "identified_by, collected_by, collection_date, specimen_voucher, sequence_md5, scientific_name, "
          + "tax_id, altitude, sex) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
  public static final String SQL_SELECT =
      "SELECT accession,\n"
          + "       location,\n"
          + "       country,\n"
          + "       identified_by,\n"
          + "       collected_by,\n"
          + "       collection_date,\n"
          + "       specimen_voucher,\n"
          + "       sequence_md5,\n"
          + "       scientific_name,\n"
          + "       tax_id,\n"
          + "       altitude,\n"
          + "       sex\n"
          + "FROM embl_data\n"
          + "WHERE accession in (\n"
          + "    SELECT min(accession) as a\n"
          + "    FROM embl_data\n"
          + "    GROUP BY scientific_name, collection_date, location, specimen_voucher\n"
          + ")";

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
          DwcTerm.sex);
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

  private EmblAdapterConstants() {}
}
