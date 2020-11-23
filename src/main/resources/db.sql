CREATE TABLE embl_data
(
    accession        text PRIMARY KEY,
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
    sex              text
);
