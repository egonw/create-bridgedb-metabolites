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
  "chebi_metabolites", new DataDerby(), DBConnector.PROP_RECREATE
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
database.setInfo("DATASOURCENAME", "CHEBI 1");
database.setInfo("DATASOURCEVERSION", "chebi_metabolites_" + dateStr);
database.setInfo("DATATYPE", "Metabolite");
database.setInfo("SERIES", "standard_metabolite");

def addXRef(GdbConstruct database, Xref ref, String node, DataSource source) {
   id = node.trim()
   if (id.length() > 0) {
     println "id: $id"
     ref2 = new Xref(id, source);
     database.addGene(ref2);
     database.addLink(ref, ref2);
   }
}

chebiFile = "data/chebi_database_accession.tsv"
chebisDone = new HashSet();
counter = 0
new File(chebiFile).eachLine { line ->
  if (!line.startsWith("ID")) {
    fields = line.split('\t')
    String rootid = fields[1]
    Xref ref = new Xref(rootid, BioDataSource.CHEBI);
    error = 0
    if (!chebisDone.contains(rootid)) {
      error += database.addGene(ref);// FIXME: need to test if already exists
      if (error > 0) println "errors: $error chebi $rootid -> " + database.recentException().message
      error += database.addLink(ref,ref);
      if (error > 0) println "errors: $error chebi $rootid -> " + database.recentException().message
      altId = "CHEBI:$rootid"
      chebiAlt = new Xref(altId, BioDataSource.CHEBI);
      error += database.addLink(ref,chebiAlt);
      if (error > 0) println "errors: $error chebi $rootid -> " + database.recentException().message
      chebisDone.add(rootid);
    } else {
      // already done
    }
    dbSource = fields[3]
    dbId = fields[4]
    if (dbSource == "KEGG COMPOUND accession") {
      link = new Xref(dbId, keggDS);
      error += database.addLink(ref,link);
    } else if (dbSource == "KEGG DRUG accession") {
      link = new Xref(dbId, keggDrugDS);
      error += database.addLink(ref,link);
    } else if (dbSource == "CAS Registry Number") {
      link = new Xref(dbId, casDS);
      error += database.addLink(ref,link);
    } else if (dbSource == "HMDB accession") {
      link = new Xref(dbId, BioDataSource.HMDB);
      error += database.addLink(ref,link);
    } else if (dbSource == "Chemspider accession") {
      link = new Xref(dbId, chemspiderDS);
      error += database.addLink(ref,link);
    } else if (dbSource == "Wikipedia accession") {
      link = new Xref(dbId, wikipediaDS);
      error += database.addLink(ref,link);
    }
    if (error > 0) println "errors: $error dbid $dbId -> " + database.recentException().message
    println "errors: " + error

    counter++
    if (counter % 100 == 0) database.commit()
  }
}

database.commit();
database.finalize();
