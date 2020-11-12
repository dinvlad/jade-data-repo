package bio.terra.integration;

import bio.terra.common.category.Integration;
import bio.terra.common.configuration.TestConfiguration;
import bio.terra.common.fixtures.Names;
import bio.terra.model.*;
import bio.terra.service.iam.IamRole;

import org.junit.After;
import org.junit.Before;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"google", "integrationtest"})
@AutoConfigureMockMvc
@Category(Integration.class)
public class DatasetBucketLinkTest extends UsersBase {

    private static Logger logger = LoggerFactory.getLogger(FileTest.class);

    @Autowired
    private DataRepoFixtures dataRepoFixtures;

    @Autowired
    private DataRepoClient dataRepoClient;

    @Autowired
    private TestConfiguration testConfiguration;

    private DatasetSummaryModel datasetSummaryModel;
    private DatasetSummaryModel secondDatasetSummaryModel;
    private String datasetId;
    private String secondDatasetId;
    private String profileId;
    private BillingProfileModel billingProfileModel1;
    private BillingProfileModel billingProfileModel2;

    @Before
    public void setup() throws Exception {
        logger.info("[BUCKET_TESTING]: --------------------Setup start----------------");
        super.setup();

    }

    @After
    public void tearDown() throws Exception {
        if (datasetId != null) {
            dataRepoFixtures.deleteDataset(steward(), datasetId);
        }
        if (secondDatasetId != null) {
            dataRepoFixtures.deleteDataset(steward(), secondDatasetId);
        }
    }


    @Test
    public void sharedProfileBucketLinkTest() throws Exception {
        profileId = dataRepoFixtures.createBillingProfile(steward()).getId();

        // create first dataset w/ profile
        datasetSummaryModel = dataRepoFixtures.createDataset(steward(), "bucket-link-test-dataset.json");
        datasetId = datasetSummaryModel.getId();
        logger.info("created dataset " + datasetId);
        dataRepoFixtures.addDatasetPolicyMember(
            steward(), datasetSummaryModel.getId(), IamRole.CUSTODIAN, custodian().getEmail());

        // create second dataset w/ profile
        secondDatasetSummaryModel = dataRepoFixtures.createDataset(steward(), "bucket-link-test-dup-dataset.json");
        secondDatasetId = secondDatasetSummaryModel.getId();
        logger.info("created second dataset " + secondDatasetId);
        dataRepoFixtures.addDatasetPolicyMember(
            steward(), secondDatasetSummaryModel.getId(), IamRole.CUSTODIAN, custodian().getEmail());

        final int filesToLoad = 2;

        String loadTag = Names.randomizeName("bucketLinkTest");

        BulkLoadArrayRequestModel
            arrayLoad = new BulkLoadArrayRequestModel()
            .profileId(profileId)
            .loadTag(loadTag)
            .maxFailedFileLoads(filesToLoad); // do not stop if there is a failure.

        logger.info("longFileLoadTest loading " + filesToLoad + " files into dataset id " + datasetId);

        for (int i = 0; i < filesToLoad; i++) {
            String tailPath = String.format("/fileloadscaletest/file1GB-%02d.txt", i);
            String sourcePath = "gs://jade-testdata-uswestregion" + tailPath;
            String targetPath = "/" + loadTag + tailPath;

            BulkLoadFileModel model = new BulkLoadFileModel().mimeType("application/binary");
            model.description("bulk load file " + i)
                .sourcePath(sourcePath)
                .targetPath(targetPath);
            arrayLoad.addLoadArrayItem(model);
        }

        BulkLoadArrayResultModel result = dataRepoFixtures.bulkLoadArray(steward(), datasetId, arrayLoad);
        result.getLoadFileResults().forEach(r -> logger.info("Bucket for dataset 1: " + r.getTargetPath()));

        BulkLoadResultModel loadSummary = result.getLoadSummary();
        logger.info("Total files    : " + loadSummary.getTotalFiles());
        logger.info("Succeeded files: " + loadSummary.getSucceededFiles());
        logger.info("Failed files   : " + loadSummary.getFailedFiles());
        logger.info("Not Tried files: " + loadSummary.getNotTriedFiles());

        BulkLoadArrayResultModel secondResult = dataRepoFixtures.bulkLoadArray(steward(), secondDatasetId, arrayLoad);
        secondResult.getLoadFileResults().forEach(r -> logger.info("Bucket for dataset 2: " + r.getTargetPath()));
        BulkLoadResultModel secondLoadSummary = secondResult.getLoadSummary();
        logger.info("Total files    : " + secondLoadSummary.getTotalFiles());
        logger.info("Succeeded files: " + secondLoadSummary.getSucceededFiles());
        logger.info("Failed files   : " + secondLoadSummary.getFailedFiles());
        logger.info("Not Tried files: " + secondLoadSummary.getNotTriedFiles());
    }

    @Test
    public void twoProfileBucketLinkTest() throws Exception {
        logger.info("[BUCKET_TESTING]: --------------------Test start----------------");
        billingProfileModel1 = dataRepoFixtures.createBillingProfile(steward());
        logger.info("Profile 1: {}", billingProfileModel1.toString());

        billingProfileModel2 = dataRepoFixtures.createBillingProfile(custodian());
        logger.info("Profile 2: {}", billingProfileModel2.toString());


        // create first dataset w/ profile 1
        datasetSummaryModel = dataRepoFixtures.createDatasetWithProfile(
            steward(), billingProfileModel1, "bucket-link-test-dataset.json");
        datasetId = datasetSummaryModel.getId();
        logger.info("created dataset " + datasetId);
        dataRepoFixtures.addDatasetPolicyMember(
            steward(), datasetSummaryModel.getId(), IamRole.CUSTODIAN, custodian().getEmail());

        // create second dataset w/ profile 2
        secondDatasetSummaryModel = dataRepoFixtures.createDatasetWithProfile(
            steward(), billingProfileModel2, "bucket-link-test-dup-dataset.json");
        secondDatasetId = secondDatasetSummaryModel.getId();
        logger.info("created second dataset " + secondDatasetId);
        dataRepoFixtures.addDatasetPolicyMember(
            steward(), secondDatasetSummaryModel.getId(), IamRole.CUSTODIAN, custodian().getEmail());

        final int filesToLoad = 2;

        String loadTag = Names.randomizeName("bucketLinkTest");

        BulkLoadArrayRequestModel
            arrayLoad = new BulkLoadArrayRequestModel()
            .loadTag(loadTag)
            .maxFailedFileLoads(filesToLoad); // do not stop if there is a failure.

        logger.info("longFileLoadTest loading " + filesToLoad + " files into dataset id " + datasetId);

        for (int i = 0; i < filesToLoad; i++) {
            String tailPath = String.format("/fileloadscaletest/file1GB-%02d.txt", i);
            String sourcePath = "gs://jade-testdata-uswestregion" + tailPath;
            String targetPath = "/" + loadTag + tailPath;

            BulkLoadFileModel model = new BulkLoadFileModel().mimeType("application/binary");
            model.description("bulk load file " + i)
                .sourcePath(sourcePath)
                .targetPath(targetPath);
            arrayLoad.addLoadArrayItem(model);
        }

        arrayLoad.setProfileId(billingProfileModel1.getId());
        BulkLoadArrayResultModel result = dataRepoFixtures.bulkLoadArray(steward(), datasetId, arrayLoad);
        result.getLoadFileResults().forEach(r -> logger.info("Bucket for dataset 1, profile 1: " + r.getTargetPath()));

        BulkLoadResultModel loadSummary = result.getLoadSummary();
        logger.info("Total files    : " + loadSummary.getTotalFiles());
        logger.info("Succeeded files: " + loadSummary.getSucceededFiles());
        logger.info("Failed files   : " + loadSummary.getFailedFiles());
        logger.info("Not Tried files: " + loadSummary.getNotTriedFiles());

        arrayLoad.setProfileId(billingProfileModel2.getId());
        BulkLoadArrayResultModel secondResult = dataRepoFixtures.bulkLoadArray(steward(), secondDatasetId, arrayLoad);
        secondResult.getLoadFileResults().forEach(r -> logger.info("Bucket for dataset 2, profile 2: " + r.getTargetPath()));
        BulkLoadResultModel secondLoadSummary = secondResult.getLoadSummary();
        logger.info("Total files    : " + secondLoadSummary.getTotalFiles());
        logger.info("Succeeded files: " + secondLoadSummary.getSucceededFiles());
        logger.info("Failed files   : " + secondLoadSummary.getFailedFiles());
        logger.info("Not Tried files: " + secondLoadSummary.getNotTriedFiles());
    }
}
