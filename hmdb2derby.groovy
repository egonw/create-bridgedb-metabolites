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

commitInterval = 500
genesDone = new java.util.HashSet();

GdbConstruct database = GdbConstructImpl3.createInstance(
  "hmdb_chebi_wikidata_metabolites", new DataDerby(), DBConnector.PROP_RECREATE
);
database.createGdbTables();
database.preInsert();

blacklist = new HashSet<String>();
blacklist.add("C00350")
blacklist.add("C00157")
blacklist.add("C00422")
blacklist.add("C00165")
blacklist.add("C02530")
blacklist.add("C00416")
blacklist.add("C02737")
blacklist.add("363-24-6")
blacklist.add("104404-17-3")
blacklist.add("CHEBI:17636")
blacklist.add("HMDB00912") // see bug #6

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
dtxDS = DataSource.register ("Dtx", "EPA CompTox Dashboard").asDataSource()
// drugbankDS = BioDataSource.DRUGBANK
wikipediaDS = BioDataSource.WIKIPEDIA

String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
database.setInfo("BUILDDATE", dateStr);
database.setInfo("DATASOURCENAME", "HMDB-CHEBI-WIKIDATA");
database.setInfo("DATASOURCEVERSION", "HMDB3.6-CHEBI140-WIKIDATA20160604" + dateStr);
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
   // println "attrib($key): $id"
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

// load the HMDB content
counter = 0
def zipFile = new java.util.zip.ZipFile(new File('hmdb_metabolites.zip'))
zipFile.entries().each { entry ->
   if (!entry.isDirectory() && entry.name != "hmdb_metabolites.xml") {
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
     if (!blacklist.contains(rootid)) {
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
         if (!blacklist.contains(keggID)) {
           addXRef(database, ref, keggID, keggDS, genesDone);
         } else {
           println "No external IDs added for: " + keggID
         }
       } else if (keggID.length() > 0 && keggID.charAt(0) == 'D') {
         addXRef(database, ref, keggID, keggDrugDS, genesDone);
       }
       addXRef(database, ref, rootNode.wikipedia.toString(), wikipediaDS, genesDone);
//      addXRef(database, ref, rootNode.nugowiki.toString(), nugoDS);
//      addXRef(database, ref, rootNode.drugbank_id.toString(), drugbankDS);
//      addXRef(database, ref, rootNode.inchi.toString(), inchiDS);
     } else {
       println "No external IDs added for: " + rootid
     }

     if (error > 0) println "errors: " + error + " (HMDB: ${entry.name})"
     counter++
     if (counter % commitInterval == 0) database.commit()
  }
}

// load the ChEBI content
counter = 0
// load the names
def chebiNames = new File('data/chebi_names.tsv')
chebiNames.eachLine { line->
  error = 0
  columns = line.split('\t')
  shortid = columns[1]
  rootid = "CHEBI:" + shortid
  name = columns[4]
  // println rootid + " -> " + name
  Xref shortRef = new Xref(shortid, BioDataSource.CHEBI);
  if (!genesDone.contains(shortRef.toString())) {
    addError = database.addGene(shortRef);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError += database.addLink(shortRef,shortRef);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    genesDone.add(shortRef.toString())
  }
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
  addAttribute(database, shortRef, "Synonym", name);
  addAttribute(database, ref, "Synonym", name);

  if (error > 0) println "errors: " + error + " (ChEBI: $rootid)"
  counter++
  if (counter % commitInterval == 0) database.commit()
}
// load the mappings
def mappedIDs = new File('data/chebi_database_accession.tsv')
mappedIDs.eachLine { line->
  columns = line.split('\t')
  rootid = "CHEBI:" + columns[1]
  type = columns[3]
  error = 0
  if (!blacklist.contains(rootid)) {
    id = columns[4]
    println "$rootid -($type)-> $id"
    Xref ref = new Xref(rootid, BioDataSource.CHEBI);
    if (type == "CAS Registry Number") {
      if (!id.contains(" ") && !id.contains(":") && id.contains("-")) {
        addXRef(database, ref, id, BioDataSource.CAS, genesDone);
      }
    } else if (type == "KEGG COMPOUND accession") {
      addXRef(database, ref, id, BioDataSource.KEGG_COMPOUND, genesDone);
    } else if (type == "Chemspider accession") {
      addXRef(database, ref, id, chemspiderDS, genesDone);
    } else if (type == "Wikipedia accession") {
      addXRef(database, ref, id, wikipediaDS, genesDone);
    } else if (type == "Pubchem accession") {
      addXRef(database, ref, id, pubchemDS, genesDone);
    } else if (type == "LIPID MAPS class accession") {
      addXRef(database, ref, id, lmDS, genesDone);
    } else if (type == "LIPID MAPS instance accession") {
      addXRef(database, ref, id, lmDS, genesDone);
    } else if (type == "KNApSAcK accession") {
      addXRef(database, ref, id, knapsackDS, genesDone);
    }
  } else {
    println "No external IDs added for: " + rootid
  }
  counter++
  if (error > 0) println "errors: " + error + " (ChEBI: $rootid)"
  if (counter % commitInterval == 0) {
    database.commit()
  }
}

// load the Wikidata content

// CAS registry numbers
counter = 0
error = 0
genesDone = new java.util.HashSet();
new File("cas2wikidata.csv").eachLine { line ->
  if (counter > 0) {
    fields = line.split(",")
    rootid = fields[0].substring(31)
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
  }
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
  if (counter > 0) {
    fields = line.split(",")
    rootid = fields[0].substring(31)
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
  }
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
  if (counter > 0) {
    fields = line.split(",")
    rootid = fields[0].substring(31)
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
      println "unclear KEGG ID ($rootid): " + keggID
    }
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
  if (counter > 0) {
    fields = line.split(",")
    rootid = fields[0].substring(31)
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
  }
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
  if (counter > 0) {
    fields = line.split(",")
    rootid = fields[0].substring(31)
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
    addXRef(database, ref, fields[1], lmDS, genesDone);
  }
  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (LIPIDMAPS)"
    database.commit()
  }
}

// HMDB registry numbers
counter = 0
error = 0
new File("hmdb2wikidata.csv").eachLine { line ->
  if (counter > 0) {
    fields = line.split(",")
    rootid = fields[0].substring(31)
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
    addXRef(database, ref, fields[1], BioDataSource.HMDB, genesDone);
  }
  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (HMDB)"
    database.commit()
  }
}

// ChEBI registry numbers
counter = 0
error = 0
new File("chebi2wikidata.csv").eachLine { line ->
  if (counter > 0) {
    fields = line.split(",")
    rootid = fields[0].substring(31)
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
    shortid = fields[1]
    chebiid = "CHEBI:" + shortid
    Xref chebiRef = new Xref(rootid, BioDataSource.CHEBI);
    addXRef(database, ref, shortid, BioDataSource.CHEBI, genesDone);
    addXRef(database, ref, chebiid, BioDataSource.CHEBI, genesDone);
    addXRef(database, chebiRef, rootid, wikidataDS, genesDone);
  }
  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (ChEBI)"
    database.commit()
  }
}

// Wikidata names
counter = 0
error = 0
new File("names4wikidata.tsv").eachLine { line ->
  if (counter > 0) {
    fields = line.split("\t")
    if (fields.length >= 3) {
      rootid = fields[0].substring(31)
      key = fields[1].trim()
      synonym = fields[2].trim()
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
      if (synonym.length() > 0) {
        addAttribute(database, ref, "Symbol", synonym)
        addXRef(database, ref, key, inchikeyDS, genesDone);
      }
      if (key.length() > 0) {
        addAttribute(database, ref, "InChIKey", key);
      }
    }
  }
  counter++
  if (counter % commitInterval == 0) {
    println "errors: " + error + " (label)"
    database.commit()
  }
}

database.commit();
database.finalize();
