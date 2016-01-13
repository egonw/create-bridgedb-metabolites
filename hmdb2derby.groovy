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
  "hmdb_chebi_wikidata_metabolites", new DataDerby(), DBConnector.PROP_RECREATE
);
database.createGdbTables();
database.preInsert();

//inchiDS = DataSource.register ("Cin", "InChI").asDataSource()
inchikeyDS = DataSource.register ("Ik", "InChIKey").asDataSource()
chemspiderDS = DataSource.register ("Cs", "Chemspider").asDataSource()
casDS = BioDataSource.CAS
pubchemDS = BioDataSource.PUBCHEM_COMPOUND
chebiDS = BioDataSource.CHEBI
keggDS = BioDataSource.KEGG_COMPOUND
keggDrugDS = DataSource.register ("Kd", "KEGG Drug").asDataSource()
wikidataDS = DataSource.register ("Wd", "Wikidata").asDataSource()
lmDS = DataSource.register ("Lm", "LIPID MAPS").asDataSource()
knapsackDS = DataSource.register ("Cks", "KNApSAcK").asDataSource()
// drugbankDS = BioDataSource.DRUGBANK
wikipediaDS = BioDataSource.WIKIPEDIA

String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
database.setInfo("BUILDDATE", dateStr);
database.setInfo("DATASOURCENAME", "HMDB-CHEBI-WIKIDATA");
database.setInfo("DATASOURCEVERSION", "HMDB3.6-CHEBI142-WIKIDATA20150401" + dateStr);
database.setInfo("DATATYPE", "Metabolite");
database.setInfo("SERIES", "standard_metabolite");

def addXRef(GdbConstruct database, Xref ref, String node, DataSource source, Set genesDone) {
   id = node.trim()
   if (id.length() > 0) {
     // println "id($source): $id"
     ref2 = new Xref(id, source);
     if (!genesDone.contains(ref2.toString())) {
       if (database.addGene(ref2)) println "Error (addGene): " + database.recentException().getMessage()
       genesDone.add(ref2.toString())
     }
     if (database.addLink(ref, ref2)) println "Error (addLink): " + database.recentException().getMessage()
   }
}

def addAttribute(GdbConstruct database, Xref ref, String key, String value) {
   id = value.trim()
   println "attrib($key): $id"
   if (id.length() > 255) {
     println "Warn: attribute does not fit the Derby SQL schema: $id"
   } else if (id.length() > 0) {
     if (database.addAttribute(ref, key, value) != 0) {
       println "Error (addAttrib): " + database.getException().getMessage()
     }
   }
}

def cleanKey(String inchikey) {
   String cleanKey = inchikey.trim()
   if (cleanKey.startsWith("InChIKey=")) cleanKey = cleanKey.substring(9)
   cleanKey
}

commitInterval = 500

// load the HMDB content
counter = 0
genesDone = new java.util.HashSet();
def zipFile = new java.util.zip.ZipFile(new File('hmdb_metabolites.zip'))
zipFile.entries().each { entry ->
   if (!entry.isDirectory() && entry.name != "hmdb_metabolites.xml") {
     println entry.name
     inputStream = zipFile.getInputStream(entry)
     def rootNode = new XmlSlurper().parse(inputStream)
     error = 0

     String rootid = rootNode.accession.toString()
     Xref ref = new Xref(rootid, BioDataSource.HMDB);
     if (!genesDone.contains(ref.toString())) {
       addError = database.addGene(ref);
       if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
       error += addError
       linkError = database.addLink(ref,ref);
       if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
       error += linkError
       genesDone.add(ref.toString())
     }

     // add the synonyms
     addAttribute(database, ref, "Symbol", rootNode.name.toString());
     rootNode.synonyms.synonym.each { synonym ->
       addAttribute(database, ref, "Synonym", synonym.toString())
     }
     addAttribute(database, ref, "Synonym", rootNode.traditional_iupac.toString());
     addAttribute(database, ref, "Synonym", rootNode.iupac_name.toString());

     // add the SMILES, InChIKey, etc
     addAttribute(database, ref, "InChI", cleanKey(rootNode.inchi.toString()));
     key = cleanKey(rootNode.inchikey.toString().trim());
     if (key.length() == 27) {
       addAttribute(database, ref, "InChIKey", key);
       addXRef(database, ref, key, inchikeyDS, genesDone);
     }
     addAttribute(database, ref, "SMILES", rootNode.smiles.toString());
     addAttribute(database, ref, "BrutoFormula", rootNode.chemical_formula.toString());
     addAttribute(database, ref, "Taxonomy Parent", rootNode.direct_parent.toString());
     addAttribute(database, ref, "Monoisotopic Weight", rootNode.monisotopic_moleculate_weight.toString());

     // add external identifiers
     // addXRef(database, ref, rootNode.accession.toString(), BioDataSource.HMDB);
     addXRef(database, ref, rootNode.cas_registry_number.toString(), casDS, genesDone);
     addXRef(database, ref, rootNode.pubchem_compound_id.toString(), pubchemDS, genesDone);
     addXRef(database, ref, rootNode.chemspider_id.toString(), chemspiderDS, genesDone);
     String chebID = rootNode.chebi_id.toString().trim()
     if (chebID.startsWith("CHEBI:")) {
       addXRef(database, ref, chebID, chebiDS, genesDone);
       addXRef(database, ref, chebID.substring(6), chebiDS, genesDone);
     } else if (chebID.length() > 0) {
       addXRef(database, ref, chebID, chebiDS, genesDone);
       addXRef(database, ref, "CHEBI:" + chebID, chebiDS, genesDone);
     }
     String keggID = rootNode.kegg_id.toString();
     if (keggID.length() > 0 && keggID.charAt(0) == 'C') {
       addXRef(database, ref, keggID, keggDS, genesDone);
     } else if (keggID.length() > 0 && keggID.charAt(0) == 'D') {
       addXRef(database, ref, keggID, keggDrugDS, genesDone);
     }
     addXRef(database, ref, rootNode.wikipedia.toString(), wikipediaDS, genesDone);
//      addXRef(database, ref, rootNode.nugowiki.toString(), nugoDS);
//      addXRef(database, ref, rootNode.drugbank_id.toString(), drugbankDS);
//      addXRef(database, ref, rootNode.inchi.toString(), inchiDS);

     println "errors: " + error + " (ChEBI)"
     counter++
     if (counter % commitInterval == 0) database.commit()
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
  if (!genesDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError += database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(ref.toString())
  }
  addAttribute(database, ref, "Synonym", name);

  println "errors: " + error + " (ChEBI)"
  counter++
  if (counter % commitInterval == 0) database.commit()
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
    addXRef(database, ref, id, BioDataSource.CAS, genesDone);
  } else if (type == "KEGG COMPOUND accession") {
    addXRef(database, ref, id, BioDataSource.KEGG_COMPOUND, genesDone);
  } else if (type == "Chemspider accession") {
    addXRef(database, ref, id, chemspiderDS, genesDone);
  } else if (type == "Wikipedia accession") {
    addXRef(database, ref, id, wikipediaDS, genesDone);
  } else if (type == "Pubchem accession") {
    addXRef(database, ref, id, pubchemDS, genesDone);
  }
  println "errors: " + error + " (ChEBI)"
  counter++
  if (counter % commitInterval == 0) database.commit()
}

// load the Wikidata content

// CAS registry numbers
counter = 0
error = 0
genesDone = new java.util.HashSet();
new File("cas2wikidata.csv").eachLine { line ->
  fields = line.split(",")
  rootid = fields[0]
  Xref ref = new Xref(rootid, wikidataDS);
  if (!genesDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError = database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(ref.toString())
  }

  // add external identifiers
  addXRef(database, ref, fields[1], casDS, genesDone);

  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (CAS)"
    database.commit()
  }
}

// PubChem registry numbers
counter = 0
error = 0
new File("pubchem2wikidata.csv").eachLine { line ->
  fields = line.split(",")
  rootid = fields[0]
  Xref ref = new Xref(rootid, wikidataDS);
  if (!genesDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError = database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(ref.toString())
  }

  // add external identifiers
  addXRef(database, ref, fields[1], pubchemDS, genesDone);

  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (PubChem)"
    database.commit()
  }
}

// KEGG registry numbers
counter = 0
error = 0
new File("kegg2wikidata.csv").eachLine { line ->
  fields = line.split(",")
  rootid = fields[0]
  Xref ref = new Xref(rootid, wikidataDS);
  if (!genesDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError = database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(ref.toString())
  }

  // add external identifiers
  keggID = fields[1]
  if (keggID.charAt(0) == 'C') {
    addXRef(database, ref, keggID, keggDS, genesDone);
  } else if (keggID.charAt(0) == 'D') {
    addXRef(database, ref, keggID, keggDrugDS, genesDone);
  } else {
    println "unclear KEGG ID: " + keggID
  }

  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (KEGG)"
    database.commit()
  }
}

// ChemSpider registry numbers
counter = 0
error = 0
new File("cs2wikidata.csv").eachLine { line ->
  fields = line.split(",")
  rootid = fields[0]
  Xref ref = new Xref(rootid, wikidataDS);
  if (!genesDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError = database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(ref.toString())
  }

  // add external identifiers
  addXRef(database, ref, fields[1], chemspiderDS, genesDone);

  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (ChemSpider)"
    database.commit()
  }
}

// LIPID MAPS registry numbers
counter = 0
error = 0
new File("lm2wikidata.csv").eachLine { line ->
  fields = line.split(",")
  rootid = fields[0]
  Xref ref = new Xref(rootid, lmDS);
  if (!genesDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError = database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(ref.toString())
  }

  // add external identifiers
  addXRef(database, ref, fields[1], chemspiderDS, genesDone);

  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (LIPIDMAPS)"
    database.commit()
  }
}

// KNApSAcK registry numbers
counter = 0
error = 0
new File("knapsack2wikidata.csv").eachLine { line ->
  fields = line.split(",")
  rootid = fields[0]
  Xref ref = new Xref(rootid, knapsackDS);
  if (!genesDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError = database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(ref.toString())
  }

  // add external identifiers
  addXRef(database, ref, fields[1], chemspiderDS, genesDone);

  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (KNApSAcK)"
    database.commit()
  }
}

database.commit();
database.finalize();
