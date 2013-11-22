// export CLASSPATH=`ls -1 *.jar | tr '\n' ':'`

import java.text.SimpleDateFormat;
import java.util.Date;
import groovy.util.slurpersupport.NodeChildren;

import org.bridgedb.IDMapperException;
import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.construct.DBConnector;
import org.bridgedb.rdb.construct.DataDerby;
import org.bridgedb.rdb.construct.GdbConstruct;
import org.bridgedb.rdb.construct.GdbConstructImpl3;

GdbConstruct database = GdbConstructImpl3.createInstance(
  "hmdb_metabolites", new DataDerby(), DBConnector.PROP_RECREATE
);
database.createGdbTables();
database.preInsert();

//inchiDS = DataSource.register ("Cin", "InChI").asDataSource()
//inchikeyDS = DataSource.register ("Cik", "InChIKey").asDataSource()
chemspiderDS = DataSource.register ("Cs", "Chemspider").asDataSource()
casDS = BioDataSource.CAS
pubchemDS = BioDataSource.PUBCHEM_COMPOUND
chebiDS = BioDataSource.CHEBI
keggDS = BioDataSource.KEGG_COMPOUND
keggDrugDS = DataSource.register ("Kd", "KEGG Drug").asDataSource()
// drugbankDS = BioDataSource.DRUGBANK
wikipediaDS = BioDataSource.WIKIPEDIA

String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
database.setInfo("BUILDDATE", dateStr);
database.setInfo("DATASOURCENAME", "HMDB35CHEBI");
database.setInfo("DATASOURCEVERSION", "metabolites_" + dateStr);
database.setInfo("DATATYPE", "Metabolite");
database.setInfo("SERIES", "standard_metabolite");

def addXRef(GdbConstruct database, Xref ref, String node, DataSource source) {
   id = node.trim()
   if (id.length() > 0) {
     println "id: $id"
     ref2 = new Xref(id, source);
     if (database.addGene(ref2)) println "Error (addGene): " + database.getErrorMessage()
     if (database.addLink(ref, ref2)) println "Error (addLink): " + database.getErrorMessage()
   }
}

def addAttribute(GdbConstruct database, Xref ref, String key, String value) {
   id = value.trim()
   println "attrib($key): $id"
   if (id.length() > 255) {
     println "Warn: attribute does not fit the Derby SQL schema: $id"
   } else if (id.length() > 0) {
     if (database.addAttribute(ref, key, value)) println "Error (addAttrib): " + database.getErrorMessage()
   }
}

def cleanKey(String inchikey) {
   String cleanKey = inchikey.trim()
   if (cleanKey.startsWith("InChIKey=")) cleanKey = cleanKey.substring(9)
   cleanKey
}

// load the HMDB content
counter = 0
def zipFile = new java.util.zip.ZipFile(new File('hmdb_metabolites.zip'))
zipFile.entries().each { entry ->
   if (!entry.isDirectory()) {
     println entry.name
     inputStream = zipFile.getInputStream(entry)
     def rootNode = new XmlSlurper().parse(inputStream)

     String rootid = rootNode.accession.toString()
     Xref ref = new Xref(rootid, BioDataSource.HMDB);
     error = database.addGene(ref);
     error += database.addLink(ref,ref);

     // add the synonyms
     addAttribute(database, ref, "Symbol", rootNode.common_name.toString());
     rootNode.synonyms.synonym.each { synonym ->
       addAttribute(database, ref, "Synonym", synonym.toString())
     }
     addAttribute(database, ref, "Synonym", rootNode.traditional_iupac.toString());
     addAttribute(database, ref, "Synonym", rootNode.iupac_name.toString());

     // add the SMILES, InChIKey, etc
     // addAttribute(database, ref, "InChI", cleanKey(rootNode.inchi.toString()));
     addAttribute(database, ref, "InChIKey", cleanKey(rootNode.inchkey.toString()));
     addAttribute(database, ref, "SMILES", rootNode.smiles.toString());
     addAttribute(database, ref, "BrutoFormula", rootNode.chemical_formula.toString());
     addAttribute(database, ref, "Taxonomy Parent", rootNode.direct_parent.toString());
     addAttribute(database, ref, "Monoisotopic Weight", rootNode.monisotopic_moleculate_weight.toString());

     // add external identifiers
     // addXRef(database, ref, rootNode.accession.toString(), BioDataSource.HMDB);
     addXRef(database, ref, rootNode.cas_registry_number.toString(), casDS);
     addXRef(database, ref, rootNode.pubchem_compound_id.toString(), pubchemDS);
     addXRef(database, ref, rootNode.chemspider_id.toString(), chemspiderDS);
     String chebID = rootNode.chebi_id.toString().trim()
     if (chebID.startsWith("CHEBI:")) {
       addXRef(database, ref, chebID, chebiDS);
       addXRef(database, ref, chebID.substring(6), chebiDS);
     } else if (chebID.length() > 0) {
       addXRef(database, ref, chebID, chebiDS);
       addXRef(database, ref, "CHEBI:" + chebID, chebiDS);
     }
     String keggID = rootNode.kegg_id.toString();
     if (keggID.length() > 0 && keggID.charAt(0) == 'C') {
       addXRef(database, ref, keggID, keggDS);
     } else if (keggID.length() > 0 && keggID.charAt(0) == 'D') {
       addXRef(database, ref, keggID, keggDrugDS);
     }
     addXRef(database, ref, rootNode.wikipedia.toString(), wikipediaDS);
//      addXRef(database, ref, rootNode.nugowiki.toString(), nugoDS);
//      addXRef(database, ref, rootNode.drugbank_id.toString(), drugbankDS);
//      addXRef(database, ref, rootNode.inchi.toString(), inchiDS);

     println "errors: " + error
     counter++
     if (counter % 100 == 0) database.commit()
  }
}

// load the ChEBI content
counter = 0
// load the names
def chebiNames = new File('data/chebi_names.tsv')
chebiNames.eachLine { line->
  columns = line.split('\t')
  rootid = "CHEBI:" + columns[1]
  name = columns[4]
  // println rootid + " -> " + name
  Xref ref = new Xref(rootid, BioDataSource.CHEBI);
  error = database.addGene(ref);
  error += database.addLink(ref,ref);
  addAttribute(database, ref, "Synonym", name);

  println "errors: " + error
  counter++
  if (counter % 100 == 0) database.commit()
}
// load the mappings
def mappedIDs = new File('data/chebi_database_accession.tsv')
mappedIDs.eachLine { line->
  columns = line.split('\t')
  rootid = "CHEBI:" + columns[1]
  type = columns[3]
  id = columns[4]
  println "$rootid -($type)-> $id"
  error = 0
  Xref ref = new Xref(rootid, BioDataSource.CHEBI);
  if (type == "CAS Registry Number") {
    addXRef(database, ref, id, BioDataSource.CAS);
  } else if (type == "KEGG COMPOUND accession") {
    addXRef(database, ref, id, BioDataSource.KEGG_COMPOUND);
  } else if (type == "Chemspider accession") {
    addXRef(database, ref, id, chemspiderDS);
  } else if (type == "Wikipedia accession") {
    addXRef(database, ref, id, wikipediaDS);
  } else if (type == "Pubchem accession") {
    addXRef(database, ref, id, pubchemDS);
  }
  println "errors: " + error
  counter++
  if (counter % 100 == 0) database.commit()
}

database.commit();
database.finalize();
