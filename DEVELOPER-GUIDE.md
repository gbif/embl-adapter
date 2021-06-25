# Developer guide: EMBL adapter
The adapter is configured to run once a week at a specific time (might be changed in the future). 
See properties `startTime` and `frequencyInDays` in the gbif-configuration project [here](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/embl-adapter.yaml).

Basic steps of the adapter:

1) Request data from ENA portal API, two requests for each dataset + one taxonomy request (optional)
2) Store raw data into database
3) Perform backend deduplication
4) Produce DWC archive


## Requests

We get data from https://www.ebi.ac.uk/ena/portal/api. Query supports the following operators and characters: `AND`, `OR`, `NOT`, `()`, `"""`, `*`.

Requests `requestUrl1` (sequence) and `requestUrl2` (wgs_set) can be seen at gbif-configuration project [here](https://github.com/gbif/gbif-configuration/blob/master/cli/dev/config/embl-adapter.yaml).


### Sequence requests
Request with `result=sequence` 

1) **a dataset for eDNA**: environmental_sample=True, host="" (no host)
```
query=(specimen_voucher="*" OR country="*") AND dataclass!="CON" AND environmental_sample=true AND NOT host="*"
```
- include records with environmental_sample=true
- include records with coordinates and\or specimen_voucher  
- exclude records dataclass="CON" see [here](https://github.com/gbif/embl-adapter/issues/10)
- exclude records with host

2) **a dataset for organism sequenced**: environmental_sample=False, host="" (no host)
```
query=(specimen_voucher="*" OR country="*") AND dataclass!="CON" AND environmental_sample=false AND NOT host="*"
```
- include records with environmental_sample=false
- include records with coordinates and\or specimen_voucher
- exclude records dataclass="CON" see [here](https://github.com/gbif/embl-adapter/issues/10)
- exclude records with host

3) **a dataset with hosts**
```
query=(specimen_voucher="*" OR country="*") AND dataclass!="CON" AND host="*" AND NOT host="human*" AND NOT host="*Homo sa*"
```
- include records with coordinates and\or specimen_voucher
- include records with host  
- exclude records dataclass="CON" see [here](https://github.com/gbif/embl-adapter/issues/10)
- exclude records with human host


### WGS_SET request
Request with `result=wgs_set`.
These requests are pretty much the same with some differences:
- sequence_md5 field not supported, use specimen_voucher twice to match number of fields
- do not use dataclass filter


### Taxonomy
Adapter can also request taxonomy.
we request taxonomy separately (from what place? url?) and also store into database (where?)


## Database
The data is stored in the postgres database after execution. Each dataset has own table with raw data.

Database is created only once in the target environment and tables are cleaned up before every run.

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


## DWC archive
Adapter generates three DWC archives as output.
[Data mapping](DATAMAPPING.md)


## Configuration
Remember that all configuration files are in the private [gbif-configuration](https://github.com/gbif/gbif-configuration) project!

Configuration files in the directory `src/main/resources` **do not affect** the adapter and can be used, for example, for testing (local run). 

## Local run
Use scripts [start.sh](start.sh) and [start-taxonomy.sh](start-taxonomy.sh) for local testing.
Remember to provide valid logback and config files for the scripts (you may need to create databases before run).
