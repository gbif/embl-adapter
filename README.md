# EMBL adapter
Contains adapters for connecting EMBL content into GBIF.

This repository contains an EMBL API crawler that produces data that is later used for producing DwC-A files suitable for ingestion into GBIF.

The results are the four [EMBL-EBI datasets](https://www.gbif.org/dataset/search?publishing_org=ada9d123-ddb4-467d-8891-806ea8d94230).

* [INSDC Sequences](https://doi.org/10.15468/sbmztx)
* [INSDC Environment Sample Sequences](https://doi.org/10.15468/mcmd5g)
* [INSDC Host Organism Sequences](https://doi.org/10.15468/e97kmy)
* [The European Nucleotide Archive (ENA) taxonomy](https://doi.org/10.15468/avkgwm)

Expected use of the EMBL API by the crawler is described in [this working document](https://docs.google.com/document/d/1GCBHAbKZasHRQWcsZFKVRkGlzqXT9XI_dnBTJQk1zj4/edit)

The adapter is configured to run once a week at a specific time.
See the properties `startTime` and `frequencyInDays` in the [gbif-configuration project](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/embl-adapter.yaml).

Basic steps of the adapter:

1) Request data from ENA portal API, two requests for each dataset + one taxonomy request (optional)
2) Store raw data into database
3) Process and store processed data into database (Perform backend deduplication)
4) Clean temporal files


## Requests

We get data from https://www.ebi.ac.uk/ena/portal/api. See the [API documentation](https://www.ebi.ac.uk/ena/portal/api/doc) provided by EBI.

Requests `requestUrl1` (sequence) and `requestUrl2` (wgs_set) can be seen in the [gbif-configuration project](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/embl-adapter.yaml).


### Sequence requests
Request with `result=sequence`

1) **a dataset for eDNA**: environmental_sample=True, host="" (no host)
```
query=(specimen_voucher="*" OR country="*") AND dataclass!="CON" AND environmental_sample=true AND host!="*"
```
- include records with environmental_sample=true
- include records with coordinates and/or specimen_voucher
- exclude records dataclass="CON" see [here](https://github.com/gbif/embl-adapter/issues/10)
- exclude records with host

2) **a dataset for organism sequenced**: environmental_sample=False, host="" (no host)
```
query=(specimen_voucher="*" OR country="*") AND dataclass!="CON" AND environmental_sample=false AND host!="*"
```
- include records with environmental_sample=false
- include records with coordinates and/or specimen_voucher
- exclude records dataclass="CON" see [here](https://github.com/gbif/embl-adapter/issues/10)
- exclude records with host

3) **a dataset with hosts**
```
query=(specimen_voucher="*" OR country="*") AND dataclass!="CON" AND host="*" AND host!="human" AND host!="Homo sapiens" AND host!="Homo_sapiens"
```
- include records with coordinates and/or specimen_voucher
- include records with host
- exclude records dataclass="CON" see [here](https://github.com/gbif/embl-adapter/issues/10)
- exclude records with human host


### WGS_SET request
Request with `result=wgs_set`.
These requests are pretty much the same with some differences:
- sequence_md5 field not supported, use specimen_voucher twice to match number of fields
- do not use dataclass filter


### Taxonomy
Adapter requests taxonomy separately: download a zipped archive, unzip it and store it into database.
Configuration is [here](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/ena-taxonomy.yaml).


## Database
The data is stored in the PostgreSQL database after execution. Each dataset has own table with raw and processed data.

The database is created only once in the target environment and tables are cleaned up before every run.

Database creation scripts for [data](src/main/resources/db.sql) and [taxonomy](src/main/resources/ena-taxonomy-db.sql).

See gbif-configuration [here](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/embl-adapter.yaml)
and [here](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/ena-taxonomy.yaml) for connection properties.


## Backend deduplication
We perform several deduplication steps.

### First step
Perform [SQL](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/sql-dataset-common.sql)
(local copy [here](src/main/resources/sql-dataset-common.sql)),
get rid of some duplicates and join data with taxonomy;
based on issue [here](https://github.com/gbif/embl-adapter/issues/10)

### Second step
Get rid of records with both missing `specimen_voucher` and `collection_date`

### Third step
Keep only one record with same `sample_accession` and `scientific_name` and get rid of the rest


## DWC archives
Adapter stores all processed data back into database (tables with postfix `_processed`) which then used by IPT as SQL sources.

Test datasets (UAT):

- https://www.gbif-uat.org/dataset/ee8da4a4-268b-4e91-ab5a-69a04ff58e7a
- https://www.gbif-uat.org/dataset/768eeb1f-a208-4170-9335-2968d17c7bdc
- https://www.gbif-uat.org/dataset/10628730-87d4-42f5-b593-bd438185517f

and production ones (prod):

- https://www.gbif.org/dataset/583d91fe-bbc0-4b4a-afe1-801f88263016
- https://www.gbif.org/dataset/393b8c26-e4e0-4dd0-a218-93fc074ebf4e
- https://www.gbif.org/dataset/d8cd16ba-bb74-4420-821e-083f2bac17c2

[Data mapping](DATAMAPPING.md)


## Configuration
Remember that all configuration files are in the private [gbif-configuration](https://github.com/gbif/gbif-configuration) project!

Configuration files in the directory `src/main/resources` **do not affect** the adapter and can be used, for example, for testing (local run).

## Local run
Use scripts [start.sh](start.sh) and [start-taxonomy.sh](start-taxonomy.sh) for local testing.
Remember to provide valid logback and config files for the scripts (you may need to create databases before run).
