Create BridgeDB Identity Mapping files for HMDB
===============================================

This groovy script is loosly build on code developed by Martina Kutmon [1]
and creates a Derby file for BridgeDB [2] for use in PathVisio, etc.

The script has been tested with HMDB 3.0 [3] data from January 2013.

License
-------

New BSD.

Run the script
--------------

1. add the jars to your classpath, e.g. on Linux with:

  export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`

2. make sure the HMDB data file is saved as hmdb_metabolites.zip

3. run the script with Groovy:

  groovy hmdb2derby.groovy

4. open the file in PathVisio

References
----------

1. http://svn.bigcat.unimaas.nl/bridgedbcreator/trunk/src/org/bridgedb/creator/
2. http://bridgedb.org/
3. http://hmdb.ca/
