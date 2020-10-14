package scripts.testscripts.spendtracker;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import common.utils.BigQueryUtils;
import common.utils.FileUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.TestUserSpecification;

public class SpendDataBigQueryJob extends runner.TestScript {
  private static final Logger logger = LoggerFactory.getLogger(SpendDataBigQueryJob.class);

  protected static String projectId;

  protected static List<String> queries =
      Arrays.asList(
          new String[] {
            "select * from bigquery-public-data.human_genome_variants.1000_genomes_sample_info",
            "select * from bigquery-public-data.human_genome_variants.1000_genomes_pedigree"
          });

  public void setParameters(List<String> parameters) {
    if (parameters == null || parameters.size() < 1) {
      throw new IllegalArgumentException("Project id must be specified as the first parameter.");
    }
    projectId = parameters.get(0);
    logger.info("projectId = {}", projectId);
  }

  public void userJourney(TestUserSpecification testUser) throws Exception {
    BigQuery bigQueryClient = BigQueryUtils.getClientForTestUser(testUser, projectId);

    // define the labels to use for each query job
    Map<String, String> labels = new HashMap<>();
    labels.put("terra-client", "tdr");
    labels.put("tdr-dataset", FileUtils.randomizeName("1000genomes-"));
    labels.put("tdr-reader", testUser.name);

    // run the queries
    for (String query : queries) {
      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(query).setLabels(labels).build();
      TableResult queryResult = bigQueryClient.query(queryConfig);
      logger.info("Query found {} total rows", queryResult.getTotalRows());
    }
  }
}
