SELECT subq1.accession,
       subq1.location,
       subq1.country,
       subq1.identified_by,
       subq1.collected_by,
       subq1.collection_date,
       subq1.specimen_voucher,
       subq1.sequence_md5,
       subq1.scientific_name,
       subq1.tax_id,
       subq1.altitude,
       subq1.sex,
       subq1.description
FROM (
         SELECT row_number() OVER (PARTITION BY left(accession, 6) ORDER BY accession) as row_number,
                accession,
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
         FROM (SELECT *
               FROM embl_data
               WHERE accession SIMILAR TO ('[A-Z]{4}[0-9]{2}S?[0-9]{6,8}')) as regex_matched
     ) as subq1
WHERE subq1.row_number = 1
UNION ALL
SELECT subq1.accession,
       subq1.location,
       subq1.country,
       subq1.identified_by,
       subq1.collected_by,
       subq1.collection_date,
       subq1.specimen_voucher,
       subq1.sequence_md5,
       subq1.scientific_name,
       subq1.tax_id,
       subq1.altitude,
       subq1.sex,
       subq1.description
FROM (
         SELECT row_number() OVER (PARTITION BY left(accession, 8) ORDER BY accession) as row_number,
                accession,
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
         FROM (SELECT *
               FROM embl_data
               WHERE accession SIMILAR TO ('[A-Z]{6}[0-9]{2}S?[0-9]{7,9}')) as regex_matched
     ) as subq1
WHERE subq1.row_number = 1
UNION ALL
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
WHERE accession NOT SIMILAR TO ('[A-Z]{4}[0-9]{2}S?[0-9]{6,8}')
  AND accession NOT SIMILAR TO ('[A-Z]{6}[0-9]{2}S?[0-9]{7,9}');
