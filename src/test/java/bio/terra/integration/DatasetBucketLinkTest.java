package bio.terra.integration;

import bio.terra.common.category.Integration;
import bio.terra.common.configuration.TestConfiguration;
import bio.terra.common.fixtures.Names;
import bio.terra.model.BulkLoadArrayRequestModel;
import bio.terra.model.BulkLoadArrayResultModel;
import bio.terra.model.BulkLoadFileModel;
import bio.terra.model.BulkLoadResultModel;
import bio.terra.model.DatasetSummaryModel;
import bio.terra.model.ErrorModel;
import bio.terra.model.FileModel;
import bio.terra.model.IngestRequestModel;
import bio.terra.model.IngestResponseModel;
import bio.terra.model.JobModel;
import bio.terra.service.iam.IamRole;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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

    @Before
    public void setup() throws Exception {
        super.setup();

    }

    @After
    public void tearDown() throws Exception {
        if (datasetId != null) {
            dataRepoFixtures.deleteDataset(steward(), datasetId);
        }
        if (secondDatasetId != null) {
            dataRepoFixtures.deleteDataset(steward(), datasetId);
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
        secondDatasetId = datasetSummaryModel.getId();
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

        BulkLoadResultModel loadSummary = result.getLoadSummary();
        logger.info("Total files    : " + loadSummary.getTotalFiles());
        logger.info("Succeeded files: " + loadSummary.getSucceededFiles());
        logger.info("Failed files   : " + loadSummary.getFailedFiles());
        logger.info("Not Tried files: " + loadSummary.getNotTriedFiles());

        BulkLoadArrayResultModel secondResult = dataRepoFixtures.bulkLoadArray(steward(), secondDatasetId, arrayLoad);
        BulkLoadResultModel secondLoadSummary =secondResult.getLoadSummary();
        logger.info("Total files    : " + secondLoadSummary.getTotalFiles());
        logger.info("Succeeded files: " + secondLoadSummary.getSucceededFiles());
        logger.info("Failed files   : " + secondLoadSummary.getFailedFiles());
        logger.info("Not Tried files: " + secondLoadSummary.getNotTriedFiles());
    }




}
