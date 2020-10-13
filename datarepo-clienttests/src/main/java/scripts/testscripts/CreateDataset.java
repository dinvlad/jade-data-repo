package scripts.testscripts;

import bio.terra.datarepo.api.RepositoryApi;
import bio.terra.datarepo.client.ApiClient;
import bio.terra.datarepo.model.BulkLoadArrayRequestModel;
import bio.terra.datarepo.model.BulkLoadArrayResultModel;
import bio.terra.datarepo.model.BulkLoadFileModel;
import bio.terra.datarepo.model.DatasetSummaryModel;
import bio.terra.datarepo.model.IngestRequestModel;
import bio.terra.datarepo.model.IngestResponseModel;
import bio.terra.datarepo.model.JobModel;
import com.google.cloud.storage.BlobId;
import common.utils.FileUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.TestUserSpecification;
import scripts.testscripts.baseclasses.CreateProfile;
import scripts.utils.BulkLoadUtils;
import scripts.utils.DataRepoUtils;
import scripts.utils.SAMUtils;

public class CreateDataset extends CreateProfile {
  private static final Logger logger = LoggerFactory.getLogger(CreateDataset.class);

  List<String> filePathsToIngest = new ArrayList<>();
  static Map<DatasetSummaryModel, TestUserSpecification> datasetsCreated = new HashMap<>();
  private static List<BlobId> scratchFiles = new ArrayList<>();

  /* Pass in a list of file paths to ingest into the dataset. List may be empty. */
  public void setParameters(List<String> parameters) throws Exception {
    if (parameters != null && parameters.size() > 0) {
      filePathsToIngest = parameters;
    }
    logger.info("Load {} files", filePathsToIngest.size());
  }

  public void setup(List<TestUserSpecification> testUsers) throws Exception {
    // check that all test users are Data Repo stewards in SAM
    for (TestUserSpecification testUser : testUsers) {
      if (!SAMUtils.isDataRepoSteward(
          SAMUtils.getClientForTestUser(testUser, server), server.samResourceIdForDatarepo)) {
        throw new RuntimeException(
            "Test user "
                + testUser.name
                + " is not a Data Repo steward in SAM, so it cannot create a dataset.");
      }
    }

    // create a billing profile
    super.setup(testUsers);
  }

  /* Create a dataset and ingest files from the paths specified in the parameters list. */
  public void userJourney(TestUserSpecification testUser) throws Exception {
    // get the ApiClient for the dataset creator
    ApiClient datasetCreatorClient = DataRepoUtils.getClientForTestUser(testUser, server);
    RepositoryApi repositoryApi = new RepositoryApi(datasetCreatorClient);

    // make the create dataset request and wait for the job to finish
    JobModel createDatasetJobResponse =
        DataRepoUtils.createDataset(
            repositoryApi, billingProfileModel.getId(), "dataset-simple.json", true);
    DatasetSummaryModel datasetSummaryModel =
        DataRepoUtils.expectJobSuccess(
            repositoryApi, createDatasetJobResponse, DatasetSummaryModel.class);
    logger.info("Successfully created dataset: {}", datasetSummaryModel.getId());

    // store a reference to the dataset and the test user that created it, so we can delete it
    // during the cleanup step
    datasetsCreated.put(datasetSummaryModel, testUser);

    // ingest any files
    if (filePathsToIngest.size() > 0) {
      // build the bulk load request
      String loadTag = FileUtils.randomizeName("CreateDatasetTest");
      BulkLoadArrayRequestModel fileLoadModelArray =
          new BulkLoadArrayRequestModel()
              .profileId(datasetSummaryModel.getDefaultProfileId())
              .loadTag(loadTag)
              .maxFailedFileLoads(0);
      for (String filePathToIngest : filePathsToIngest) {
        String targetPath = "/testrunner/CreateDataset/" + FileUtils.randomizeName("") + ".txt";
        BulkLoadFileModel fileLoadModel =
            new BulkLoadFileModel()
                .sourcePath(filePathToIngest)
                .description(filePathToIngest)
                .mimeType("text/plain")
                .targetPath(targetPath);
        fileLoadModelArray.addLoadArrayItem(fileLoadModel);
      }

      // submit the bulk load request and wait for it to finish
      JobModel ingestFileJobResponse =
          repositoryApi.bulkFileLoadArray(datasetSummaryModel.getId(), fileLoadModelArray);
      ingestFileJobResponse =
          DataRepoUtils.waitForJobToFinish(repositoryApi, ingestFileJobResponse);
      BulkLoadArrayResultModel bulkLoadArrayResultModel =
          DataRepoUtils.expectJobSuccess(
              repositoryApi, ingestFileJobResponse, BulkLoadArrayResultModel.class);

      // build the tabular data ingest request
      String fileRefName =
          "scratch/CreateDataset/" + FileUtils.randomizeName("tabularDataIngestRequest") + ".json";
      BlobId tabularDataIngestFile =
          BulkLoadUtils.writeScratchFileForIngestRequest(
              server.testRunnerServiceAccount,
              bulkLoadArrayResultModel,
              "jade-testdata",
              fileRefName);
      IngestRequestModel ingestRequest =
          BulkLoadUtils.makeIngestRequestFromScratchFile(tabularDataIngestFile);
      scratchFiles.add(tabularDataIngestFile); // make sure the scratch file gets cleaned up later

      // submit the tabular data ingest request and wait for it to finish
      JobModel ingestTabularDataJobResponse =
          repositoryApi.ingestDataset(datasetSummaryModel.getId(), ingestRequest);
      ingestTabularDataJobResponse =
          DataRepoUtils.waitForJobToFinish(repositoryApi, ingestTabularDataJobResponse);
      IngestResponseModel ingestResponse =
          DataRepoUtils.expectJobSuccess(
              repositoryApi, ingestTabularDataJobResponse, IngestResponseModel.class);
      logger.info("Successfully loaded data into dataset: {}", ingestResponse.getDataset());
    }
  }

  public void cleanup(List<TestUserSpecification> testUsers) throws Exception {
    //    // delete the datasets created by the user journey threads
    //    for (Map.Entry<DatasetSummaryModel, TestUserSpecification> datasetCreated :
    //        datasetsCreated.entrySet()) {
    //      // get the ApiClient for the dataset creator
    //      TestUserSpecification datasetCreator = datasetCreated.getValue();
    //      ApiClient datasetCreatorClient = DataRepoUtils.getClientForTestUser(datasetCreator,
    // server);
    //      RepositoryApi repositoryApi = new RepositoryApi(datasetCreatorClient);
    //
    //      // make the delete dataset request and wait for the job to finish
    //      DatasetSummaryModel datasetSummaryModel = datasetCreated.getKey();
    //      JobModel deleteDatasetJobResponse =
    // repositoryApi.deleteDataset(datasetSummaryModel.getId());
    //      deleteDatasetJobResponse =
    //          DataRepoUtils.waitForJobToFinish(repositoryApi, deleteDatasetJobResponse);
    //      DataRepoUtils.expectJobSuccess(
    //          repositoryApi, deleteDatasetJobResponse, DeleteResponseModel.class);
    //      logger.info("Successfully deleted dataset: {}", datasetSummaryModel.getId());
    //    }
    //
    //    // delete any scratch files used for ingesting tabular data
    //    StorageUtils.deleteFiles(
    //        StorageUtils.getClientForServiceAccount(server.testRunnerServiceAccount),
    // scratchFiles);
    //
    //    // delete the billing profile
    //    super.cleanup(testUsers);
  }
}
