Create BridgeDB Identity Mapping files for HMDB
===============================================

This groovy script is loosly build on code developed by Martina Kutmon [1]
and creates a Derby file for BridgeDB [2] for use in PathVisio, etc.

The script has been tested with HMDB 3.6 [3], ChEBI 138, and Wikidata from April 2016.

I'm indebted to all that worked on identifier mappings in these projects:

- http://hmdb.ca/
- http://www.ebi.ac.uk/chebi/
- http://wikidata.org/

Everyone can contribute ID mappings to the latter project.

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

4. make sure the Wikidata file is saved

Run the following SPARQL queries at http://query.wikidata.org/ and save the
output as CSV files [4]:

4.1 CAS registry numbers

SPARQL query of which the output is to be saved as "cas2wikidata.csv":

  ```
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound ?id WHERE {
    ?compound wdt:P231 ?id .
  }
  ```

4.2 ChemSpider

SPARQL query of which the output is to be saved as "cs2wikidata.csv":

  ```
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound ?id WHERE {
    ?compound wdt:P661 ?id .
  }
  ```

4.3 PubChem CIDs

SPARQL query of which the output is to be saved as "pubchem2wikidata.csv":

  ```
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound ?id WHERE {
    ?compound wdt:P662 ?id .
  }
  ```

4.4 KEGG compound IDs

SPARQL query of which the output is to be saved as "kegg2wikidata.csv":

  ```
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound ?id WHERE {
    ?compound wdt:P665 ?id .
  }
  ```

4.5 KnAPSaCK IDs

SPARQL query of which the output is to be saved as "ksnapsack2wikidata.csv":

  ```
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound ?id WHERE {
    ?compound wdt:P2064 ?id .
  }
  ```

4.6 LIPIDMAP IDs

SPARQL query of which the output is to be saved as "lm2wikidata.csv":

  ```
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound ?id WHERE {
    ?compound wdt:P2063 ?id .
  }
  ```

5. run the script with Groovy:

  groovy hmdb2derby.groovy

6. open the file in PathVisio

References
----------

1. http://svn.bigcat.unimaas.nl/bridgedbcreator/trunk/src/org/bridgedb/creator/
2. http://bridgedb.org/
3. http://hmdb.ca/
4. https://chem-bla-ics.blogspot.nl/2015/12/new-edition-getting-cas-registry.html
