package scripts.testscripts.spendtracker;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import common.utils.BigQueryUtils;
import common.utils.FileUtils;
import common.utils.LabelUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.TestUserSpecification;

public class SpendDataBigQuery extends runner.TestScript {
  private static final Logger logger = LoggerFactory.getLogger(SpendDataBigQuery.class);

  protected static String projectId;
  protected static BigQuery bigQueryClient;

  protected static Map<String, String> sourceDataQueries = new HashMap<>();

  public void setParameters(List<String> parameters) {
    sourceDataQueries.put(
        "1000_genomes_sample_info",
        "select * from bigquery-public-data.human_genome_variants.1000_genomes_sample_info");
    sourceDataQueries.put(
        "1000_genomes_pedigree",
        "select * from bigquery-public-data.human_genome_variants.1000_genomes_pedigree");

    if (parameters == null || parameters.size() < 1) {
      throw new IllegalArgumentException("Project id must be specified as the first parameter.");
    }
    projectId = parameters.get(0);
    logger.info("projectId = {}", projectId);
  }

  public void setup(List<TestUserSpecification> testUsers) throws Exception {
    // get the bigquery client using the service account's credentials
    // this would be the service that originally creates the dataset (e.g. RBS)
    bigQueryClient =
        BigQueryUtils.getClientForServiceAccount(server.testRunnerServiceAccount, projectId);
  }

  public void userJourney(TestUserSpecification testUser) throws Exception {
    // generate a random dataset name
    String generatedDatasetName = FileUtils.randomizeName("spendtrackerpoca_");
    logger.info("generatedDatasetName = {}", generatedDatasetName);

    // labels to set on creation
    Map<String, String> labels = new HashMap<>();
    labels.put("terra-service", "tdr");
    labels.put("terra-creator", server.testRunnerServiceAccount.name);

    // create a new dataset
    Dataset dataset =
        bigQueryClient.create(
            DatasetInfo.newBuilder(generatedDatasetName)
                .setLocation("US")
                .setLabels(LabelUtils.validateLabelMap(labels, true))
                .build());
    logger.info("Created dataset {}, {}", dataset.getDatasetId().getDataset(), dataset.getLabels());

    // update the labels after the dataset has been created
    labels = new HashMap<>();
    labels.putAll(dataset.getLabels());
    labels.put("tdr-dataset", FileUtils.randomizeName("datasetid-"));
    labels.put("tdr-billingprofile", FileUtils.randomizeName("billingprofileid-"));
    labels.put("tdr-creator", testUser.name);
    dataset =
        dataset.toBuilder().setLabels(LabelUtils.validateLabelMap(labels, true)).build().update();
    logger.info("Updated labels on dataset {}", dataset.getDatasetId().getDataset());

    // add a table
    for (Map.Entry<String, String> sourceQuery : sourceDataQueries.entrySet()) {
      String tableName = sourceQuery.getKey();
      String queryStr = sourceQuery.getValue();

      TableId tableId =
          TableId.of(dataset.getDatasetId().getDataset(), FileUtils.randomizeName(tableName));
      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(queryStr).setDestinationTable(tableId).build();

      // create the table and get a reference to it
      bigQueryClient.query(queryConfig);
      Table table = bigQueryClient.getTable(tableId);

      // update the labels after the table has been created
      labels = new HashMap<>();
      labels.putAll(table.getLabels());
      labels.put("source-table", tableName);
      table =
          table
              .toBuilder()
              .setDescription(queryStr)
              .setLabels(LabelUtils.validateLabelMap(labels, true))
              .build()
              .update();

      logger.info("Created and populated table: {}", table.getTableId().getTable());
    }
  }

  public void cleanup(List<TestUserSpecification> testUsers) throws Exception {
    // don't delete the dataset because we want to incur spend data
  }
}
