// export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`

// config stuff
def predicate = "skos:relatedMatch"
def version = "20170209"
def uploadLocation = "http://egonw.github.io/create-bridgedb-hmdb/wikidata/$version/"


import java.text.SimpleDateFormat;
import java.util.Date;
import groovy.util.slurpersupport.NodeChildren;
import org.codehaus.groovy.runtime.DateGroovyMethods;

def wikidataNS = "http://wikidata.org/entity/"

// configuring things
datasets = [
  [
    name: "ChemSpider",
    acronym: "cs",
    landingPage: "http://chemspider.com/",
    targetNSprefix: "http://rdf.chemspider.com/",
    targetNSpostfix: "",
    objectsTarget: "<ftp://ftp.rsc-us.org/OPS/20130408/void_2013-04-08.ttl#chemSpiderDataset>",
    field: "chemspider_id",
    extraVoID: ""
  ]
]

dateTime = new Date()
current_date = DateGroovyMethods.format(dateTime, "yyyy-MM-dd'T'HH:mm:ss");

def voidFilename = "wikidata_ls.void.ttl"
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
  dcterms:title "A VoID Description of the Wikidata LinkSets" ;
  dcterms:description "VoID file describing the link set defined by Wikidata." ;
  pav:createdBy <http://egonw.github.com/#me> ;
  pav:createdOn "2013-05-27T18:49:00Z"^^xsd:dateTime ;
  pav:createdWith <https://github.com/egonw/create-bridgedb-hmdb/> ;
  pav:lastUpdateOn "${current_date}Z"^^xsd:dateTime ;
  dcterms:issued "${current_date}Z"^^xsd:dateTime ;
  foaf:primaryTopic :Wikidata .

:Wikidata a void:Dataset ;
  dcterms:publisher <wikidata.org> ;
  dcterms:title "Wikidata" ;
  dcterms:description "Wikidata is a generic database with also information about metabolites, drugs and (other) xenobiotics found in the many organisms."@en;
  dcterms:license <http://creativecommons/cczero>;
  foaf:homepage <http://wikidata.org/>;
  dcat:landingPage <http://wikidata.org>;
  dcterms:issued "2015-01-01T00:00:00Z"^^xsd:dateTime ;
  pav:retrievedBy <http://egonw.github.com/#me> ;
  pav:retrievedFrom <http://wikidata.org> ;
  pav:retrievedOn "2015-08-26T09:21:00Z"^^xsd:dateTime ;
  void:uriSpace <wikidataNS> ;
  void:exampleResource <http://wikidata.org/entity/Q5> ;
  dcterms:accuralPeriodicity <http://purl.org/cld/freq/irregular> ;
  pav:version "${version}".
"""

// loop over all data sets
for (i in 0..(datasets.size()-1)) {
  def lsFilename = "wikidata_ls_${datasets[i].acronym}.ttl"
  def lsvoidFilename = "wikidata_ls_${datasets[i].acronym}.void.ttl"

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

<${uploadLocation}${voidFilename}#Wikidata> void:subset :LS-${lsCode} .

:LS-${lsCode} a void:Linkset ;
  dcterms:title "Wikidata to ${datasets[i].name} LinkSet" ;
  dcterms:description "A link set with links between Wikidata and ${datasets[i].name} entries."@en;
  dcterms:license <http://creativecommons/cczero>;
  void:dataDump <${uploadLocation}${lsFilename}> ;
  pav:version "$version"^^xsd:string ;
  pav:createdBy <http://egonw.github.com/#me> ;
  dcterms:publisher <http://egonw.github.com/#me> ;
  pav:createdOn "${current_date}Z"^^xsd:dateTime ;
  pav:createdWith <https://github.com/egonw/create-bridgedb-hmdb/> ;
  void:linkPredicate $predicate ;
  bdb:linksetJustification <http://semanticscience.org/resource/SIO_001171> ;
  void:subjectsTarget <${uploadLocation}${voidFilename}#Wikidata> ;
  bdb:subjectsDatatype <http://semanticscience.org/resource/SIO_010004> ;
  void:objectsTarget ${datasets[i].objectsTarget} ;
  bdb:objectsDatatype <http://semanticscience.org/resource/SIO_010004> ;
  voag:frequencyOfChange <http://purl.org/cld/freq/irregular> ;
  pav:authoredBy <http://wikidata.org/>;
  pav:authoredOn "2013-05-29T10:02:00Z"^^xsd:dateTime .
"""

  if (datasets[i].extraVoID.length() > 0)
    lsvoidOut.println datasets[i].extraVoID

  def tripleCount = 0
  // ChemSpider registry numbers
  error = 0
  new File("cs2wikidata.csv").eachLine { line,number ->
    if (number == 1) return // skip the first line

    fields = line.split(",")
    rootid = fields[0]
    identifier = fields[1]
    lsOut.println "<" + rootid + "> $predicate <" +
      datasets[i].targetNSprefix + identifier + datasets[i].targetNSpostfix +
      "> ."
    tripleCount++
  }

  lsvoidOut.println ":LS-${lsCode} void:triples $tripleCount ."
  lsvoidOut.close()
  lsOut.close()

  voidOut.println ":Wikidata void:subset <${uploadLocation}${lsvoidFilename}#LS-${lsCode}> ."
}

voidOut.close()
