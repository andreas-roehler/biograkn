package grakn.biograkn.migrator.geneidentification;

import ai.grakn.GraknTxType;
import ai.grakn.client.Grakn;
import ai.grakn.graql.Graql;
import ai.grakn.graql.InsertQuery;
import ai.grakn.graql.answer.ConceptMap;
import grakn.biograkn.migrator.disease.Disease;
import grakn.biograkn.migrator.gene.Gene;
import grakn.biograkn.migrator.person.Person;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static ai.grakn.graql.Graql.var;

public class GeneIdentification {

    public static void migrate(Grakn.Session session) {
        try {
            BufferedReader reader = Files.newBufferedReader(Paths.get("dataset/disgenet/gene_identification.csv"));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

            for (CSVRecord csvRecord: csvParser) {

                // skip header
                if (csvRecord.getRecordNumber() == 1) {
                    continue;
                }

                double personId = Double.parseDouble(csvRecord.get(0));
                String geneSymbol = csvRecord.get(1);

                InsertQuery insertQuery = Graql.match(
                        var("p").isa("person").has("person-id", personId),
                        var("g").isa("gene").has("gene-symbol", geneSymbol))
                        .insert(var("gi").isa("gene-identification").rel("genome-owner", "p").rel("identified-gene", "g"));

                Grakn.Transaction writeTransaction = session.transaction(GraknTxType.WRITE);
                List<ConceptMap> insertedIds = insertQuery.withTx(writeTransaction).execute();

                if (insertedIds.isEmpty()) {
                    List<Class> prereqs = Arrays.asList(Person.class, Gene.class);
                    throw new IllegalStateException("Nothing was inserted for: " + insertQuery.toString() +
                            "\nA prerequisite dataset may have not been loaded. This dataset requires: " + prereqs.toString());
                }

                System.out.println("Inserted a gene identification with ID: " + insertedIds.get(0).get("gi").id());
                writeTransaction.commit();
            }

            System.out.println("-----gene identifications have been migrated-----");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}