-- IMPORTANT: table name will be replaced with the property tasks[].tableName
SELECT ed.accession,
       ed.sample_accession,
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
       ed.host,
       et.kingdom,
       et.phylum,
       et.class,
       et."order",
       et.family,
       et.genus
FROM (
         SELECT row_number()
                over (PARTITION BY tax_id, scientific_name, collection_date, location, country, collected_by, identified_by, sample_accession ORDER BY tax_id) as row_num_1,
                row_number()
                over (PARTITION BY scientific_name, specimen_voucher) as row_num_2,
                accession,
                sample_accession,
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
                description,
                host
         FROM embl_data
         WHERE scientific_name != 'Homo sapiens') as ed
         LEFT JOIN ena_taxonomy et ON ed.tax_id = et.taxon_id
WHERE ed.row_num_1 < 50 AND (ed.row_num_2 = 1 OR ed.specimen_voucher = '')
