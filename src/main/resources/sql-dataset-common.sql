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
       et.kingdom,
       et.phylum,
       et.class,
       et."order",
       et.family,
       et.genus
FROM embl_data ed
         LEFT JOIN ena_taxonomy et ON ed.tax_id = et.taxon_id
WHERE ed.scientific_name != 'Homo sapiens'
