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

def zipFile = new java.util.zip.ZipFile(new File('hmdb_metabolites.zip'))

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
// drugbankDS = BioDataSource.DRUGBANK
wikipediaDS = BioDataSource.WIKIPEDIA

String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
database.setInfo("BUILDDATE", dateStr);
database.setInfo("DATASOURCENAME", "HMDB3");
database.setInfo("DATASOURCEVERSION", "hmdb_metabolites_" + dateStr);
database.setInfo("DATATYPE", "Metabolite");
database.setInfo("SERIES", "standard_metabolite");

def addXRef(GdbConstruct database, Xref ref, String node, DataSource source) {
   id = node.trim()
   println "id: $id"
   if (id.length() > 0) {
     ref2 = new Xref(id, source);
     database.addGene(ref2);
     database.addLink(ref, ref2);
   }
}

def addAttribute(GdbConstruct database, Xref ref, String key, String value) {
   id = value.trim()
   println "attrib($key): $id"
   if (id.length() > 0) {
     database.addAttribute(ref, key, value);
   }
}

def cleanKey(String inchikey) {
   String cleanKey = inchikey.trim()
   if (cleanKey.startsWith("InChIKey=")) cleanKey = cleanKey.substring(9)
   cleanKey
}

counter = 0
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
     addAttribute(database, ref, "InChI", cleanKey(rootNode.inchi.toString()));
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
     addXRef(database, ref, rootNode.chebi_id.toString(), chebiDS);
     addXRef(database, ref, rootNode.kegg_id.toString(), keggDS);
     addXRef(database, ref, rootNode.wikipedia.toString(), wikipediaDS);
//      addXRef(database, ref, rootNode.nugowiki.toString(), nugoDS);
//      addXRef(database, ref, rootNode.drugbank_id.toString(), drugbankDS);
//      addXRef(database, ref, rootNode.inchi.toString(), inchiDS);

     println "errors: " + error
     
     counter++
     if (counter % 100 == 0) database.commit()
  }
}

database.commit();
database.finalize();
