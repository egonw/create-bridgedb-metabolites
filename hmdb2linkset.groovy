// export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;
import groovy.util.slurpersupport.NodeChildren;
import org.codehaus.groovy.runtime.DateGroovyMethods;

def zipFile = new java.util.zip.ZipFile(new File('hmdb_metabolites.zip'))

def predicate = "skos:relatedMatch"
def version = "3.6.0"
def uploadLocation = "http://egonw.github.io/create-bridgedb-hmdb/hmdb/$version/"

// configuring things
datasets = [
  [
    name: "ChemSpider",
    acronym: "chemspider",
    landingPage: "http://chemspider.com/",
    targetNSprefix: "http://rdf.chemspider.com/",
    targetNSpostfix: "",
    objectsTarget: "<ftp://ftp.rsc-us.org/OPS/20130408/void_2013-04-08.ttl#chemSpiderDataset>",
    field: "chemspider_id",
    extraVoID: ""
  ],
  [
    name: "CAS",
    acronym: "cas",
    targetNSprefix: "http://identifiers.org/cas/",
    targetNSpostfix: "",
    objectsTarget: ":CAS",
    field: "cas_registry_number",
    extraVoID: """
:CAS a dctypes:Dataset ;
  dcterms:title "CAS Common Chemistry" ;
  dcat:landingPage <http://commonchemistry.org/> .
"""
  ],
  [
    name: "PubChem Compounds",
    acronym: "pubchem",
    targetNSprefix: "http://pubchem.ncbi.nlm.nih.gov/rest/rdf/compound/CID",
    targetNSpostfix: "",
    objectsTarget: ":PubChem",
    field: "pubchem_compound_id",
    extraVoID: """
:PubChem a dctypes:Dataset ;
  dcterms:title "PubChem Compounds" ;
  dcat:landingPage <http://pubchem.ncbi.nlm.nih.gov/> .
"""
  ],
  [
    name: "DBPedia",
    acronym: "wp",
    targetNSprefix: "http://dbpedia.org/resource/",
    targetNSpostfix: "",
    objectsTarget: ":WP",
    field: "wikipidia",
    extraVoID: """
:WP a dctypes:Dataset ;
  dcterms:title "Wikipedia via DBPedia" ;
  dcat:landingPage <http://dbpedia.org/> .
"""
  ],
  [
    name: "KEGG",
    acronym: "kegg",
    targetNSprefix: "http://identifiers.org/kegg.compound/",
    targetNSpostfix: "",
    objectsTarget: ":KEGG",
    field: "kegg_id",
    extraVoID: """
:KEGG a dctypes:Dataset ;
  dcterms:title "KEGG" ;
  dcat:landingPage <http://www.genome.jp/kegg/> .
"""
  ]
]

dateTime = new Date()
current_date = DateGroovyMethods.format(dateTime, "yyyy-MM-dd'T'HH:mm:ss");

def hmdbNS = "http://identifiers.org/hmdb/"

def voidFilename = "hmdb_ls.void.ttl"
def voidFile = new File(voidFilename)
def voidOut = new PrintStream(voidFile.newOutputStream())
voidOut.println """
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix dctypes: <http://purl.org/dc/dcmitype/> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix pav: <http://purl.org/pav/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix voag: <http://voag.linkedmodel.org/schema/voag#> .
@prefix void: <http://rdfs.org/ns/void#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix dcat: <http://www.w3.org/ns/dcat#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> .
@prefix freq: <http://purl.org/cld/freq/> .
@prefix cheminf: <http://semanticscience.org/resource/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .

@prefix : <#> .

<> a void:DatasetDescription;
  dcterms:title "A VoID Description of the HMDB LinkSets" ;
  dcterms:description "VoID file describing the link set defined by HMDB." ;
  pav:createdBy <http://egonw.github.com/#me> ;
  pav:createdOn "2013-05-27T18:49:00Z"^^xsd:dateTime ;
  pav:createdWith <https://github.com/egonw/create-bridgedb-hmdb/> ;
  pav:lastUpdateOn "${current_date}Z"^^xsd:dateTime ;
  dcterms:issued "${current_date}Z"^^xsd:dateTime ;
  foaf:primaryTopic :HMDB .

:HMDB a void:Dataset ;
  dcterms:publisher <http://www.hmdb.ca/> ;
  dcterms:title "Human Metabolite Database" ;
  dcterms:description "The Human Metabolite Database (HMDB) is a database with information about metabolites, drugs and (other) xenobiotics found in the human organism. It is described in this paper: Wishart DS, Jewison T, Guo AC, Wilson M, Knox C, et al., HMDB 3.0 - The Human Metabolome Database in 2013. Nucleic Acids Res. 2013. Jan 1;41(D1):D801-7."@en;
  dcterms:license <http://www.hmdb.ca/citing>;
  foaf:homepage <http://www.hmdb.ca/>;
  dcat:landingPage <http://www.hmdb.ca/>;
  dcterms:issued "2015-01-01T00:00:00Z"^^xsd:dateTime ;
  pav:retrievedBy <http://egonw.github.com/#me> ;
  pav:retrievedFrom <http://www.hmdb.ca/downloads> ;
  pav:retrievedOn "2015-08-26T09:21:00Z"^^xsd:dateTime ;
  void:uriSpace <$hmdbNS> ;
  void:exampleResource <http://identifiers.org/hmdb/HMDB00005> ;
  dcterms:accuralPeriodicity <http://purl.org/cld/freq/irregular> ;
  pav:version "3.6".
"""

// loop over all data sets
for (i in 0..(datasets.size()-1)) {
  def lsFilename = "hmdb_ls_${datasets[i].acronym}.ttl"
  def lsvoidFilename = "hmdb_ls_${datasets[i].acronym}.void.ttl"

  def lsFile = new File(lsFilename)
  def lsOut = new PrintStream(lsFile.newOutputStream())
  def lsvoidFile = new File(lsvoidFilename)
  def lsvoidOut = new PrintStream(lsvoidFile.newOutputStream())
  
  def lsCode = datasets[i].acronym.toUpperCase()

  dateTime = new Date()
  current_date = DateGroovyMethods.format(dateTime, "yyyy-MM-dd'T'HH:mm:ss");

  lsOut.println """
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix void: <http://rdfs.org/ns/void#> .

<${uploadLocation}${lsFilename}> void:inDataset <${uploadLocation}${lsvoidFilename}#LS-${lsCode}> .
"""
  lsvoidOut.println """
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix dctypes: <http://purl.org/dc/dcmitype/> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix pav: <http://purl.org/pav/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix voag: <http://voag.linkedmodel.org/schema/voag#> .
@prefix void: <http://rdfs.org/ns/void#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix dcat: <http://www.w3.org/ns/dcat#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> .
@prefix freq: <http://purl.org/cld/freq/> .
@prefix cheminf: <http://semanticscience.org/resource/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix bdb: <http://vocabularies.bridgedb.org/ops#> .

@prefix : <#> .

<${uploadLocation}${voidFilename}#HMDB> void:subset :LS-${lsCode} .

:LS-${lsCode} a void:Linkset ;
  dcterms:title "HMDB to ${datasets[i].name} LinkSet" ;
  dcterms:description "A link set with links between HMDB and ${datasets[i].name} entries."@en;
  dcterms:license <http://www.hmdb.ca/citing>;
  void:dataDump <${uploadLocation}${lsFilename}> ;
  pav:version "$version"^^xsd:string ;
  pav:createdBy <http://egonw.github.com/#me> ;
  dcterms:publisher <http://egonw.github.com/#me> ;
  pav:createdOn "${current_date}Z"^^xsd:dateTime ;
  pav:createdWith <https://github.com/egonw/create-bridgedb-hmdb/> ;
  void:linkPredicate $predicate ;
  bdb:linksetJustification <http://semanticscience.org/resource/SIO_001171> ;
  void:subjectsTarget <${uploadLocation}${voidFilename}#HMDB> ;
  bdb:subjectsDatatype <http://semanticscience.org/resource/SIO_010004> ;
  void:objectsTarget ${datasets[i].objectsTarget} ;
  bdb:objectsDatatype <http://semanticscience.org/resource/SIO_010004> ;
  voag:frequencyOfChange <http://purl.org/cld/freq/irregular> ;
  pav:authoredBy <http://www.hmdb.ca/>;
  pav:authoredOn "2013-05-29T10:02:00Z"^^xsd:dateTime .
"""

  if (datasets[i].extraVoID.length() > 0)
    lsvoidOut.println datasets[i].extraVoID

  def tripleCount = 0
  zipFile.entries().each { entry ->
  // counter = 10;
  // entries = zipFile.entries().toSet().iterator()
  // while (counter > 0) {
  //  counter = counter - 1
  //  entry = entries.next()
    if (!entry.isDirectory() && entry.name != "hmdb_metabolites.xml") {
      inputStream = zipFile.getInputStream(entry)
      def rootNode = new XmlSlurper().parse(inputStream)

      String rootid = rootNode.accession.toString()

      // add external identifiers
      identifier = rootNode[datasets[i].field].toString().trim()
      if ("Not available".equals(identifier)) identifier = ""
      if (datasets[i].acronym == "wp")  {
        identifier = identifier.replace(" ", "_")
      }
      if (identifier.length() > 0) {
        tripleCount++
        lsOut.println "<" + hmdbNS + rootid + "> $predicate <" +
          datasets[i].targetNSprefix + identifier + datasets[i].targetNSpostfix +
          "> ."
      }
    }
  }

  lsvoidOut.println ":LS-${lsCode} void:triples $tripleCount ."
  lsvoidOut.close()
  lsOut.close()

  voidOut.println ":HMDB void:subset <${uploadLocation}${lsvoidFilename}#LS-${lsCode}> ."
}

voidOut.close()
