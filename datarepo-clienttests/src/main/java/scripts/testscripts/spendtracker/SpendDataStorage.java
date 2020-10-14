package scripts.testscripts.spendtracker;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import common.utils.FileUtils;
import common.utils.LabelUtils;
import common.utils.StorageUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.TestUserSpecification;

public class SpendDataStorage extends runner.TestScript {
  private static final Logger logger = LoggerFactory.getLogger(SpendDataStorage.class);

  protected static String projectId;
  protected static Storage storageClient;

  protected static String sourceBucketName;
  protected static List<String> sourceFileNames =
      Arrays.asList(
          new String[] {
            "100Bfile.txt",
            "1KBfile.txt",
            "10KBfile.txt",
            "100KBfile.txt",
            "1MBfile.txt",
            "10MBfile.txt",
            "100MBfile.txt",
            "1GBfile.txt",
            "10GBfile.txt"
          });

  public void setParameters(List<String> parameters) {
    if (parameters == null || parameters.size() < 2) {
      throw new IllegalArgumentException(
          "Two parameters are required: project id, source bucket name.");
    }
    projectId = parameters.get(0);
    sourceBucketName = parameters.get(1);
    logger.info("projectId = {}", projectId);
    logger.info("sourceBucketName = {}", sourceBucketName);
  }

  public void setup(List<TestUserSpecification> testUsers) throws Exception {
    // get the storage client using the service account's credentials
    // this would be the service that originally creates the bucket (e.g. RBS)
    storageClient =
        StorageUtils.getClientForServiceAccount(server.testRunnerServiceAccount, projectId);
  }

  public void userJourney(TestUserSpecification testUser) throws Exception {
    // generate a random bucket name
    String generatedBucketName = FileUtils.randomizeName("spendtrackerpoca-");
    logger.info("generatedBucketName = {}", generatedBucketName);

    // labels to set on creation
    Map<String, String> labels = new HashMap<>();
    labels.put("terra-service", "tdr");
    labels.put("terra-creator", server.testRunnerServiceAccount.name);

    // create a new bucket
    Bucket bucket =
        storageClient.create(
            BucketInfo.newBuilder(generatedBucketName)
                .setStorageClass(StorageClass.STANDARD)
                .setLocation("US")
                .setLabels(LabelUtils.validateLabelMap(labels, true))
                .build());
    logger.info("Created bucket {}, {}", bucket.getName(), bucket.getLabels());

    // update the labels after the bucket has been created
    labels = new HashMap<>();
    labels.putAll(bucket.getLabels());
    labels.put("tdr-dataset", FileUtils.randomizeName("datasetid-"));
    labels.put("tdr-billingprofile", FileUtils.randomizeName("billingprofileid-"));
    labels.put("tdr-creator", testUser.name);
    bucket =
        bucket.toBuilder().setLabels(LabelUtils.validateLabelMap(labels, true)).build().update();
    logger.info("Updated labels on bucket {}", bucket.getName());

    // write files to the bucket
    for (int ctr = 0; ctr < sourceFileNames.size(); ctr++) {
      String sourceFileName = sourceFileNames.get(ctr);
      Blob sourceBlob = storageClient.get(BlobId.of(sourceBucketName, sourceFileName));

      CopyWriter copyWriter = sourceBlob.copyTo(BlobId.of(bucket.getName(), sourceFileName));
      Blob targetBlob = copyWriter.getResult();
      logger.info(
          "Wrote file to bucket: {}, {} KB", targetBlob.getName(), targetBlob.getSize() / 1000);
    }
  }

  public void cleanup(List<TestUserSpecification> testUsers) throws Exception {
    // don't delete the bucket because we want to incur spend data
  }
}
