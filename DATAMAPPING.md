# Suggested mapping to DwC

List of available fields from the ENA portal API [here](https://www.ebi.ac.uk/ena/portal/api/returnFields?dataPortal=ena&format=json&result=sequence)

Initially we should use the following fields:

ENA field name | DwC | suggested formatting | Comments
------------ | ------------- | ------------- | -------------
`accession` | `occurrenceID` | | This is the primary key
`accession` | `associatedSequences` | https://www.ebi.ac.uk/ena/browser/api/embl/value | 
`accession` | `references` | https://www.ebi.ac.uk/ena/browser/view/value | 
`location` | `decimalLatitude`, `decimalLongitude` | | contains both lat and lon, must be splitted
`country` | `country`, `locality`| | Has format `<country>:<locality>` , must be splitted
`identified_by` | `identifiedBy` | | 
`collected_by` | `recordedBy` | |
`collection_date` | `eventDate` | 
`specimen_voucher` | `catalogNumber` | |
`specimen_voucher` | `basisOfRecord` | `<value> IS NOT NULL ? "PreservedSpecimen" : "MaterialSample"` | 
`sequence_md5` | `taxonID` | ASV:`<value>` | As proposed [here](https://docs.gbif-uat.org/publishing-dna-derived-data/1.0/en/#data-mapping). Allows to search for identical sequence variants
`scientific_name` | `scientificName` | |
`tax_id` | `taxonConceptID` | https://www.ebi.ac.uk/ena/browser/view/Taxon:value | Initially we should see how far we get by just supplying scientificName. But we may need a subsequent call to their [taxonApi](https://www.ebi.ac.uk/ena/browser/api/xml/30069) to retreive higher taxonomic ranks 
`altitude` | `minimumElevationInMeters`, `maximumElevationInMeters` | | Should we populate bot max and min?
`sex` | `sex` | | 
`description` | `occurrenceRemarks` | |	
`host` | `associatedTaxa` | `"host":"Lutra lutra"` (Lutra Lutra is the name of the host in this example) | 


# Example queries:

Get data for first 100 sequences that has coordinates:

https://www.ebi.ac.uk/ena/portal/api/search?result=sequence&format=json&limit=100&query=geo_box1(-90%2C-180%2C90%2C180)&fields=sample_accession,accession,location,country,identified_by,collected_by,collection_date,specimen_voucher,sequence_md5,scientific_name,tax_id,altitude,sex,description

Get first 100 sequences that has information in the `country` field:

https://www.ebi.ac.uk/ena/portal/api/search?result=sequence&format=json&limit=100&query=country="*"&fields=sample_accession,accession,location,country,identified_by,collected_by,collection_date,specimen_voucher,sequence_md5,scientific_name,tax_id,altitude,sex,description

Get first 100 sequences that has a catalogNumber:

https://www.ebi.ac.uk/ena/portal/api/search?result=sequence&format=json&limit=100&query=%20specimen_voucher=%22*%22&fields=sample_accession,accession,location,country,identified_by,collected_by,collection_date,specimen_voucher,sequence_md5,scientific_name,tax_id,altitude,sex,description

Get first 100 sequences that has information in the `identifiedby` field:

https://www.ebi.ac.uk/ena/portal/api/search?result=sequence&format=json&limit=100&query=identified_by="*"&fields=sample_accession,accession,location,country,identified_by,collected_by,collection_date,specimen_voucher,sequence_md5,scientific_name,tax_id,altitude,sex,description

