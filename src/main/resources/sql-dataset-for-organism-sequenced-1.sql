SELECT accession,
       location,
       country,
       identified_by,
       collected_by,
       collection_date,
       specimen_voucher,
       sequence_md5,
       scientific_name,
       tax_id,
       altitude,
       sex,
       description
FROM embl_data
WHERE accession in (
    SELECT min(accession) as a
    FROM embl_data
    GROUP BY scientific_name, collection_date, location, specimen_voucher
);
