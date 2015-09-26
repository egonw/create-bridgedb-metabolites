Create BridgeDB Identity Mapping files for HMDB
===============================================

This groovy script is loosly build on code developed by Martina Kutmon [1]
and creates a Derby file for BridgeDB [2] for use in PathVisio, etc.

The script has been tested with HMDB 3.0 [3] data from January 2013.

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
  wget http://www.hmdb.ca/downloads/hmdb_metabolites.zip
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

4. run the script with Groovy:

  groovy hmdb2derby.groovy

5. open the file in PathVisio

References
----------

1. http://svn.bigcat.unimaas.nl/bridgedbcreator/trunk/src/org/bridgedb/creator/
2. http://bridgedb.org/
3. http://hmdb.ca/
