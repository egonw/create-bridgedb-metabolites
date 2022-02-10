Create BridgeDb Identity Mapping files
======================================

This [Groovy](https://groovy-lang.org/) 4.0 script creates a Derby file for BridgeDb [1,2] for use in PathVisio,
etc.

The script has been tested with HMDB 5.0 [3,4,5], ChEBI 208 [6], and Wikidata [7,8] from 2022.

I'm indebted to all that worked on identifier mappings in these projects:

- http://hmdb.ca/
- https://www.ebi.ac.uk/chebi/
- http://wikidata.org/

Everyone can contribute ID mappings to the latter project.

![](https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Wikidata_stamp.png/288px-Wikidata_stamp.png)

Releases
--------

The files are released via the BridgeDb Website: http://www.bridgedb.org/mapping-databases/hmdb-metabolite-mappings/

The mapping files are also archived on Figshare: https://figshare.com/projects/BridgeDb_metabolites/28500

License
-------

This repository: New BSD.

Derby License -> http://db.apache.org/derby/license.html
BridgeDb License -> http://www.bridgedb.org/browser/trunk/LICENSE-2.0.txt

Run the script and test the results
-----------------------------------

0. Update the [createDerby.groovy file](https://github.com/bridgedb/create-bridgedb-hmdb/blob/master/createDerby.groovy#L70) with the new version number of ChEBI and [createDerby.groovy file](https://github.com/bridgedb/create-bridgedb-hmdb/blob/master/createDerby.groovy#L78) for HMDB ("DATASOURCEVERSION" field). This information is stored as metadata, and needed for example in the BridgeDb webservice to correctly display which data is in the mapping file.

1. add the jars to your classpath, e.g. on Linux with:

```shell
export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`
```

2. make sure the HMDB data file is saved as hmdb_metabolites.zip and to create a new
   zip file will each metabolite in separate XML file:

```shell
mkdir hmdb
wget http://www.hmdb.ca/system/downloads/current/hmdb_metabolites.zip
unzip hmdb_metabolites.zip
cd hmdb
cp ../hmdb_metabolites.xml .
xml_split -v -l 1 hmdb_metabolites.xml
rm hmdb_metabolites.xml
cd ..
zip -r hmdb_metabolites_split.zip hmdb
```

3. make sure the ChEBI data file is saved

```shell
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
KnAPSaCK IDs (knapsack.rq) [9].

However, you can also use the below curl command line operations.

```shell
curl -H "Accept: text/csv" --data-urlencode query@wikidata/cas.rq -G https://beta.sparql.swisslipids.org/sparql -o cas2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/cs.rq -G https://beta.sparql.swisslipids.org/sparql -o cs2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/pubchem.rq -G https://beta.sparql.swisslipids.org/sparql -o pubchem2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/chebi.rq -G https://beta.sparql.swisslipids.org/sparql -o chebi2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/kegg.rq -G https://beta.sparql.swisslipids.org/sparql -o kegg2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/hmdb.rq -G https://beta.sparql.swisslipids.org/sparql -o hmdb2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/lm.rq -G https://beta.sparql.swisslipids.org/sparql -o lm2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/knapsack.rq -G https://beta.sparql.swisslipids.org/sparql -o knapsack2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/comptox.rq -G https://beta.sparql.swisslipids.org/sparql -o comptox2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/iuphar.rq -G https://beta.sparql.swisslipids.org/sparql -o gpl2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/chembl.rq -G https://beta.sparql.swisslipids.org/sparql -o chembl2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/drugbank.rq -G https://beta.sparql.swisslipids.org/sparql -o drugbank2wikidata.csv
curl -H "Accept: text/csv" --data-urlencode query@wikidata/swisslipids.rq -G https://beta.sparql.swisslipids.org/sparql -o swisslipids2wikidata.csv
```

Thanks to Jerven Bolleman for their SPARQL endpoint work at SIB.

4.2 Get compound labels and InChIKeys

With a similar SPARQL query (names.rq) the compounds labels (English only) and
InChIKeys can be downloaded as simple TSV and saved as "names2wikidata.tsv"
(note that this file is TAB separated):

```shell
curl -H "Accept: text/tab-separated-values" --data-urlencode query@wikidata/names.rq -G https://beta.sparql.swisslipids.org/sparql -o names2wikidata.tsv
```

5. Run the script with Groovy:

```shell
export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`
groovy createDerby.groovy
```

6. Test the resulting Derby file by opening it in PathVisio

7. Use the BridgeDb QC tool to compare it with the previous mapping file

The BridgeDb repository has a tool to perform quality control (qc) on ID
mapping files:

```shell
sh qc.sh old.bridge new.bridge
```

8. Upload the data to Figshare and update the following page for the [BridgeDb (Github) Website](https://github.com/bridgedb/data/blob/master/other.json#L20-L27) on the following item (examples provided below):
- "file":    "metabolites_20200809.bridge",
- "downloadURL": "https://ndownloader.figshare.com/files/24180464",
- "doi": "10.6084/m9.figshare.12782264.v1",
- "size": "1.84 GB"

9. Tag this repository with the DOI of the latest release.

To ensure we know exactly which repository version was used to generate
a specific release, the latest commit used for that release is tagged
with the DOI on Figshare. To list all current tags:

```shell
git tag
```

To make a new tag, run:

```shell
git tag $DOR
````

where $DOI is replaced with the DOI of the release.

10. Inform downstream projects

At least the following projects need to be informed about the availability of the new mapping database:

* BridgeDb webservice
* WikiPathways RDF generation team (Jenkins server)
* WikiPathways indexer (supporting the WikiPathways web service)

References
----------

1. http://bridgedb.org/
2. Van Iersel, M. P., Pico, A. R., Kelder, T., Gao, J., Ho, I., Hanspers, K., Conklin, B. R., Evelo, C. T., Jan. 2010. The BridgeDb framework: standardized access to gene, protein and metabolite identifier mapping services. BMC bioinformatics 11 (1), 5+. http://dx.doi.org/10.1186/1471-2105-11-5
3. http://hmdb.ca/
4. Wishart, D. S., Jewison, T., Guo, A. C. C., Wilson, M., Knox, C., Liu, Y., Djoumbou, Y., Mandal, R., Aziat, F., Dong, E., Bouatra, S., Sinelnikov, I., Arndt, D., Xia, J., Liu, P., Yallou, F., Bjorndahl, T., Perez-Pineiro, R., Eisner, R., Allen, F., Neveu, V., Greiner, R., Scalbert, A., Jan. 2013. HMDB 3.0-The human metabolome database in 2013. Nucleic acids research 41 (Database issue), D801-D807. http://dx.doi.org/10.1093/nar/gks1065
5. Wishart, D. S., Feunang, Y. D., Marcu, A., Guo, A. C., Liang, K., Vázquez-Fresno, R., Sajed, T., Johnson, D., Li, C., Karu, N., Sayeeda, Z., Lo, E., Assempour, N., Berjanskii, M., Singhal, S., Arndt, D., Liang, Y., Badran, H., Grant, J., Serra-Cayuela, A., Liu, Y., Mandal, R., Neveu, V., Pon, A., Knox, C., Wilson, M., Manach, C., Scalbert, A., Nov. 2017. HMDB 4.0: the human metabolome database for 2018. Nucleic Acids Research. http://dx.doi.org/10.1093/nar/gkx1089
6. Degtyarenko, K., de Matos, P., Ennis, M., Hastings, J., Zbinden, M., McNaught, A., Alcántara, R., Darsow, M., Guedj, M., Ashburner, M., Jan. 2008. ChEBI: a database and ontology for chemical entities of biological interest. Nucleic Acids Research 36 (suppl 1), D344-D350. http://dx.doi.org/10.1093/nar/gkm791
7. Vrandečić, Denny. "Wikidata: a new platform for collaborative data collection." Proceedings of the 21st International Conference on World Wide Web. ACM, 2012. https://doi.org/10.1145/2187980.2188242
8. Mietchen D, Hagedorn G, Willighagen E, Rico M, Gómez-Pérez A, Aibar E, Rafes K, Germain C, Dunning A, Pintscher L, Kinzler D (2015) Enabling Open Science: Wikidata for Research (Wiki4R). Research Ideas and Outcomes 1: e7573. https://doi.org/10.3897/rio.1.e7573
9. https://chem-bla-ics.blogspot.nl/2015/12/new-edition-getting-cas-registry.html
