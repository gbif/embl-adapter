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

import java.util.regex.Pattern;

public final class EmblAdapterConstants {

  public static final int DEFAULT_START_HOUR = 0;
  public static final int DEFAULT_START_MINUTE = 0;
  public static final int DEFAULT_FREQUENCY = 7;

  public static final int WRITE_BATCH_SIZE = 5000;
  public static final int READ_BATCH_SIZE = 500_000;

  public static final Pattern LOCATION_PATTERN =
      Pattern.compile("([0-9.]+)\\s+(\\w)\\s+([0-9.]+)\\s+(\\w)");

  public static final String NORTH = "N";
  public static final String SOUTH = "S";
  public static final String WEST = "W";
  public static final String EAST = "E";

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
  public static final String SQL_COLUMNS_RAW_DATA =
      "accession, sample_accession, location, country, "
          + "identified_by, collected_by, collection_date, specimen_voucher, sequence_md5, scientific_name, "
          + "tax_id, altitude, sex, description, host";
  public static final String SQL_COLUMNS_PROCESSED_DATA =
      "occurrence_id, associated_sequences, \"references\", decimal_latitude, decimal_longitude, country, locality,"
          + "identified_by, recorded_by, event_date, catalog_number, basis_of_record, taxon_id, scientific_name, "
          + "taxon_concept_id, minimum_elevation_in_meters, maximum_elevation_in_meters, sex, "
          + "occurrence_remarks, associated_taxa, kingdom, phylum, class, \"order\", family, genus";
  public static final String SQL_TEST_SELECT =
      "SELECT " + SQL_COLUMNS_RAW_DATA + " FROM embl_data LIMIT 10";
  public static final String SQL_INSERT_RAW_DATA =
      "INSERT INTO embl_data("
          + SQL_COLUMNS_RAW_DATA
          + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
          + "ON CONFLICT DO NOTHING";
  public static final String SQL_INSERT_PROCESSED_DATA =
      "INSERT INTO embl_data("
          + SQL_COLUMNS_PROCESSED_DATA
          + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
          + "ON CONFLICT DO NOTHING";
  public static final String SQL_INSERT_TAXONOMY =
      "INSERT INTO ena_taxonomy(taxon_id, kingdom, phylum, "
          + "class, \"order\", family, genus) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

  // Index constants - column index in downloaded data file
  public static final String ACCESSION_COLUMN = "accession";
  public static final String SAMPLE_ACCESSION_COLUMN = "sample_accession";
  public static final String LOCATION_COLUMN = "location";
  public static final String COUNTRY_COLUMN = "country";
  public static final String IDENTIFIED_BY_COLUMN = "identified_by";
  public static final String COLLECTED_BY_COLUMN = "collected_by";
  public static final String COLLECTION_DATE_COLUMN = "collection_date";
  public static final String SPECIMEN_VOUCHER_COLUMN = "specimen_voucher";
  public static final String SEQUENCE_MD5_COLUMN = "sequence_md5";
  public static final String SCIENTIFIC_NAME_COLUMN = "scientific_name";
  public static final String TAX_ID_COLUMN = "tax_id";
  public static final String ALTITUDE_COLUMN = "altitude";
  public static final String SEX_COLUMN = "sex";
  public static final String DESCRIPTION_COLUMN = "description";
  public static final String HOST_COLUMN = "host";

  // Index constants - column index in taxonomy database select
  public static final String TAXON_ID_COLUMN = "taxon_id";
  public static final String KINGDOM_COLUMN = "kingdom";
  public static final String PHYLUM_COLUMN = "phylum";
  public static final String CLASS_COLUMN = "class";
  public static final String ORDER_COLUMN = "order";
  public static final String FAMILY_COLUMN = "family";
  public static final String GENUS_COLUMN = "genus";

  // Index constants - column index in raw data database table
  public static final int RAW_INDEX_ACCESSION = 1;
  public static final int RAW_INDEX_SAMPLE_ACCESSION = 2;
  public static final int RAW_INDEX_LOCATION = 3;
  public static final int RAW_INDEX_COUNTRY = 4;
  public static final int RAW_INDEX_IDENTIFIED_BY = 5;
  public static final int RAW_INDEX_COLLECTED_BY = 6;
  public static final int RAW_INDEX_COLLECTION_DATE = 7;
  public static final int RAW_INDEX_SPECIMEN_VOUCHER = 8;
  public static final int RAW_INDEX_SEQUENCE_MD5 = 9;
  public static final int RAW_INDEX_SCIENTIFIC_NAME = 10;
  public static final int RAW_INDEX_TAX_ID = 11;
  public static final int RAW_INDEX_ALTITUDE = 12;
  public static final int RAW_INDEX_SEX = 13;
  public static final int RAW_INDEX_DESCRIPTION = 14;
  public static final int RAW_INDEX_HOST = 15;
  // must equal the last index
  public static final int RAW_MAX_INDEX = 15;

  // Index constants - column index in taxonomy database
  public static final int TAXONOMY_INDEX_TAXON_ID = 1;
  public static final int TAXONOMY_INDEX_KINGDOM = 2;
  public static final int TAXONOMY_INDEX_PHYLUM = 3;
  public static final int TAXONOMY_INDEX_CLASS = 4;
  public static final int TAXONOMY_INDEX_ORDER = 5;
  public static final int TAXONOMY_INDEX_FAMILY = 6;
  public static final int TAXONOMY_INDEX_GENUS = 7;

  // Index constants - column index in processed data database table
  public static final int PROCESSED_INDEX_OCCURRENCE_ID = 1;
  public static final int PROCESSED_INDEX_ASSOCIATED_SEQUENCES = 2;
  public static final int PROCESSED_INDEX_REFERENCES = 3;
  public static final int PROCESSED_INDEX_DECIMAL_LATITUDE = 4;
  public static final int PROCESSED_INDEX_DECIMAL_LONGITUDE = 5;
  public static final int PROCESSED_INDEX_COUNTRY = 6;
  public static final int PROCESSED_INDEX_LOCALITY = 7;
  public static final int PROCESSED_INDEX_IDENTIFIED_BY = 8;
  public static final int PROCESSED_INDEX_RECORDED_BY = 9;
  public static final int PROCESSED_INDEX_EVENT_DATE = 10;
  public static final int PROCESSED_INDEX_CATALOG_NUMBER = 11;
  public static final int PROCESSED_INDEX_BASIS_OF_RECORD = 12;
  public static final int PROCESSED_INDEX_TAXON_ID = 13;
  public static final int PROCESSED_INDEX_SCIENTIFIC_NAME = 14;
  public static final int PROCESSED_INDEX_TAXON_CONCEPT_ID = 15;
  public static final int PROCESSED_INDEX_MINIMUM_ELEVATION_IN_METERS = 16;
  public static final int PROCESSED_INDEX_MAXIMUM_ELEVATION_IN_METERS = 17;
  public static final int PROCESSED_INDEX_SEX = 18;
  public static final int PROCESSED_INDEX_OCCURRENCE_REMARK = 19;
  public static final int PROCESSED_INDEX_ASSOCIATED_TAXA = 20;
  public static final int PROCESSED_INDEX_KINGDOM = 21;
  public static final int PROCESSED_INDEX_PHYLUM = 22;
  public static final int PROCESSED_INDEX_CLASS = 23;
  public static final int PROCESSED_INDEX_ORDER = 24;
  public static final int PROCESSED_INDEX_FAMILY = 25;
  public static final int PROCESSED_INDEX_GENUS = 26;

  private EmblAdapterConstants() {}
}
