Create BridgeDB Identity Mapping files
======================================

This groovy script creates a Derby file for BridgeDB [1] for use in PathVisio,
etc.

The script has been tested with HMDB 3.6 [2], ChEBI 150, and Wikidata from September 2016.

I'm indebted to all that worked on identifier mappings in these projects:

- http://hmdb.ca/
- http://www.ebi.ac.uk/chebi/
- http://wikidata.org/

Everyone can contribute ID mappings to the latter project.

![](https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Wikidata_stamp.png/288px-Wikidata_stamp.png)

Releases
--------

The files are released via the BridgeDb Website: http://www.bridgedb.org/mapping-databases/hmdb-metabolite-mappings/

The mapping files are also archived on Figshare: https://figshare.com/search?q=metabolite+bridgedb+mapping+database&quick=1

License
-------

This repository: New BSD.

Derby License -> http://db.apache.org/derby/license.html
BridgeDB License -> http://www.bridgedb.org/browser/trunk/LICENSE-2.0.txt

Run the script
--------------

1. add the jars to your classpath, e.g. on Linux with:

  export CLASSPATH=\`ls -1 *.jar | tr '\n' ':'\`

2. make sure the HMDB data file is saved as hmdb_metabolites.zip

  ```
  wget http://www.hmdb.ca/system/downloads/current/hmdb_metabolites.zip
  ```

3. make sure the ChEBI data file is saved

  ```
  mkdir data
  cd data
  wget ftp://ftp.ebi.ac.uk/pub/databases/chebi/Flat_file_tab_delimited/names.tsv.gz
  gunzip names.tsv.gz
  mv names.tsv chebi_names.tsv
  wget ftp://ftp.ebi.ac.uk/pub/databases/chebi/Flat_file_tab_delimited/database_accession.tsv
  mv database_accession.tsv chebi_database_accession.tsv
  ```

4. make sure the Wikidata files are saved

4.1 ID mappings

A set of SPARQL queries have been compiled and saved in the wikidata/ folder.
These queries can be manually executed at http://query.wikidata.org/. These
queries download mappings from Wikidata for CAS registry numbers (cas.rq),
ChemSpider (cs.rq), PubChem (pubchem.rq), KEGG compounds (kegg.rq),
KnAPSaCK IDs (knapsack.rq)

However, you can also use the below curl command line operations.

  ```
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/cas.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o cas2wikidata.csv
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/cs.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o cs2wikidata.csv
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/pubchem.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o pubchem2wikidata.csv
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/chebi.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o chebi2wikidata.csv
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/kegg.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o kegg2wikidata.csv
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/hmdb.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o hmdb2wikidata.csv
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/lm.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o lm2wikidata.csv
  curl -H "Accept: text/csv" --data-urlencode query@wikidata/knapsack.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o knapsack2wikidata.csv
  ```

4.2 Get compound labels and InChIKeys

With a similar SPARQL query (names.rq) the compounds labels (English only) and
InChIKeys can be downloaded as simple TSV and saved as "names4wikidata.tsv"
(note that this file is TAB separated):

  ```
  curl -H "Accept: text/tab-separated-values" --data-urlencode query@wikidata/names.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o names2wikidata.tsv
  ```

5. run the script with Groovy:

  ```
  export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`
  groovy hmdb2derby.groovy
  ```

6. open the file in PathVisio

References
----------

1. http://bridgedb.org/
2. http://hmdb.ca/
3. https://chem-bla-ics.blogspot.nl/2015/12/new-edition-getting-cas-registry.html
