// export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;
import groovy.util.slurpersupport.NodeChildren;
import org.codehaus.groovy.runtime.DateGroovyMethods;

def zipFile = new java.util.zip.ZipFile(new File('hmdb_metabolites.zip'))

// configuring things
datasets = [
  [
    name: "ChemSpider",
    acronym: "chemspider",
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
:CAS a dcterms:Dataset ;
  dcterms:title "CAS Common Chemistry" ;
  foaf:homepage <http://commonchemistry.org/> .
"""
  ]
]
    
def hmdbNS = "http://identifiers.org/hmdb/"
def predicate = "skos:relatedMatch"

// loop over all data sets
for (i in 0..(datasets.size()-1)) {
  def voidFile = new File("hmdb_ls_${datasets[i].acronym}.void")
  def lsFile = new File("hmdb_ls_${datasets[i].acronym}.ttl")
  def voidOut = new PrintStream(voidFile.newOutputStream())
  def lsOut = new PrintStream(lsFile.newOutputStream())

  dateTime = new Date()
  current_date = DateGroovyMethods.format(dateTime, "yyyy-MM-dd'T'HH:mm:ss");

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

@prefix : <#> .

<> a void:DatasetDescription;
  dcterms:title "A VoID Description of the HMDB 2 ${datasets[i].name} LinkSet" ;
  dcterms:description "VoID file describing the link set between HMDB and ${datasets[i].name}." ;
  pav:createdBy <http://egonw.github.com/#me> ;
  pav:createdOn "2013-05-27T18:49:00Z"^^xsd:dataTime ;
  pav:lastUpdateOn "${current_date}Z"^^xsd:dateTime ;
  foaf:primaryTopic :LS .

:LS a void:Linkset ;
  dcterms:title "HMDB 2 ${datasets[i].name} LinkSet" ;
  dcterms:description "A link set with links between HMDB and ${datasets[i].name} entries."@en;
  dcterms:license <http://www.hmdb.ca/citing>;
  pav:version "3.0.1"^^xsd:string ;
  pav:createdBy <http://egonw.github.com/#me> ;
  pav:createdOn "${current_date}Z"^^xsd:dateTime ;
  void:linkPredicate $predicate ;
  dul:expresses cheminf:CHEMINF_000044 ;
  void:subjectsTarget :HMDB ;
  void:objectsTarget ${datasets[i].objectsTarget} ;
  pav:authoredBy <http://www.hmdb.ca/>;
  pav:authoredOn "2013-05-29T10:02:00Z"^^xsd:dateTime .

:HMDB a void:Dataset ;
  void:subset :LS ;
  dcterms:title "Human Metabolite Database";
  dcterms:description "The Human Metabolite Database (HMDB) is a database with information about metabolites, drugs and (other) xenobiotics found in the human organism. It is described in this paper: Wishart DS, Jewison T, Guo AC, Wilson M, Knox C, et al., HMDB 3.0—The Human Metabolome Database in 2013. Nucleic Acids Res. 2013. Jan 1;41(D1):D801-7."@en;
  dcterms:license <http://www.hmdb.ca/citing>;
  foaf:homepage <http://www.hmdb.ca/>;
  foaf:page <http://www.hmdb.ca/>;
  pav:retrievedBy <http://egonw.github.com/#me> ;
  pav:retrievedFrom <http://www.hmdb.ca/downloads> ;
  pav:retrievedOn "2013-05-27T18:49:00Z"^^xsd:dateTime ;
  void:uriSpace <$hmdbNS> ;
  void:dataDump <http://www.bigcat.unimaas.nl/~egonw/hmdb/3.0.0/hmdb_ls_${datasets[i].acronym}.ttl> ;
  void:exampleResource <http://identifiers.org/hmdb/HMDB00005> ;
  voag:frequencyOfChange <http://purl.org/cld/freq/irregular> ;
  pav:version "3".
"""
  if (datasets[i].extraVoID.length() > 0)
    voidOut.println datasets[i].extraVoID

  def tripleCount = 0
try {
  zipFile.entries().each { entry ->
    if (!entry.isDirectory()) {
      inputStream = zipFile.getInputStream(entry)
      def rootNode = new XmlSlurper().parse(inputStream)

      String rootid = rootNode.accession.toString()

      // add external identifiers
      tripleCount++
      lsOut.println "<" + hmdbNS + rootid + "> $predicate <" +
	    datasets[i].targetNSprefix + rootNode[datasets[i].field].toString() + datasets[i].targetNSpostfix +
	    "> ."
      if (tripleCount > 5) { 
	voidOut.println ":LS void:triples $tripleCount ."

	voidOut.close()
	lsOut.close()
	throw new Exception()
      }
    }
  }
} catch (Exception e) {}

  voidOut.println ":LS void:triples $tripleCount ."

  voidOut.close()
  lsOut.close()
}