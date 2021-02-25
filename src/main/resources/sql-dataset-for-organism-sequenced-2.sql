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
       subq1.description,
       et.kingdom,
       et.phylum,
       et.class,
       et."order",
       et.family,
       et.genus
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
         LEFT JOIN ena_taxonomy et ON subq1.tax_id = et.taxon_id
WHERE subq1.row_number = 1 AND subq1.scientific_name != 'Homo sapiens'
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
       subq1.description,
       et.kingdom,
       et.phylum,
       et.class,
       et."order",
       et.family,
       et.genus
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
         LEFT JOIN ena_taxonomy et ON subq1.tax_id = et.taxon_id
WHERE subq1.row_number = 1 AND subq1.scientific_name != 'Homo sapiens'
UNION ALL
SELECT ed.accession,
       ed.location,
       ed.country,
       ed.identified_by,
       ed.collected_by,
       ed.collection_date,
       ed.specimen_voucher,
       ed.sequence_md5,
       ed.scientific_name,
       ed.tax_id,
       ed.altitude,
       ed.sex,
       ed.description,
       et.kingdom,
       et.phylum,
       et.class,
       et."order",
       et.family,
       et.genus
FROM embl_data ed
         LEFT JOIN ena_taxonomy et ON ed.tax_id = et.taxon_id
WHERE accession NOT SIMILAR TO ('[A-Z]{4}[0-9]{2}S?[0-9]{6,8}')
  AND accession NOT SIMILAR TO ('[A-Z]{6}[0-9]{2}S?[0-9]{7,9}')
  AND ed.scientific_name != 'Homo sapiens'
