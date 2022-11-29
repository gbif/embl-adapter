CREATE TABLE embl_data
(
    accession        text PRIMARY KEY,
    sample_accession text,
    location         text,
    country          text,
    identified_by    text,
    collected_by     text,
    collection_date  text,
    specimen_voucher text,
    sequence_md5     text,
    scientific_name  text,
    tax_id           text,
    altitude         text,
    sex              text,
    description      text,
    host             text
);


CREATE TABLE embl_data_processed
(
    occurrence_id               text PRIMARY KEY,
    associated_sequences        text,
    "references"                text,
    decimal_latitude            text,
    decimal_longitude           text,
    country                     text,
    locality                    text,
    identified_by               text,
    recorded_by                 text,
    event_date                  text,
    catalog_number              text,
    basis_of_record             text,
    taxon_id                    text,
    scientific_name             text,
    taxon_concept_id            text,
    minimum_elevation_in_meters text,
    maximum_elevation_in_meters text,
    sex                         text,
    occurrence_remarks          text,
    associated_taxa             text,
    kingdom                     text,
    phylum                      text,
    class                       text,
    "order"                     text,
    family                      text,
    genus                       text
);
