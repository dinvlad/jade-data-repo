package bio.terra.service.dataset;

import bio.terra.common.Column;
import bio.terra.common.MetadataEnumeration;
import bio.terra.common.Table;
import bio.terra.common.category.Unit;
import bio.terra.common.fixtures.JsonLoader;
import bio.terra.common.fixtures.ProfileFixtures;
import bio.terra.common.fixtures.ResourceFixtures;
import bio.terra.model.BillingProfileModel;
import bio.terra.model.BillingProfileRequestModel;
import bio.terra.model.DatasetRequestModel;
import bio.terra.service.dataset.exception.DatasetLockException;
import bio.terra.service.dataset.exception.DatasetNotFoundException;
import bio.terra.service.profile.ProfileDao;
import bio.terra.service.resourcemanagement.ResourceService;
import bio.terra.service.resourcemanagement.google.GoogleBucketResource;
import bio.terra.service.resourcemanagement.google.GoogleProjectResource;
import bio.terra.service.resourcemanagement.google.GoogleResourceDao;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Category(Unit.class)
public class DatasetBucketDaoTest {

    @Autowired
    private JsonLoader jsonLoader;

    @Autowired
    private DatasetDao datasetDao;

    @Autowired
    private DatasetBucketDao datasetBucketDao;

    @Autowired
    private ProfileDao profileDao;

    @Autowired
    private GoogleResourceDao resourceDao;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceService resourceService;

    /*private BillingProfileModel billingProfile;
    private UUID projectId;
    private Dataset dataset;*/

    private UUID datasetId;
    private UUID bucketResourceId;

    @Test
    public void TestDatasetBucketLink() {
        datasetId = UUID.randomUUID();
        bucketResourceId = UUID.randomUUID();

        //initial check - link should not yet exist
        boolean linkExists = datasetBucketDao.datasetBucketLinkExists(datasetId, bucketResourceId);
        assertFalse("Link should not yet exist.", linkExists);

        // create link
        datasetBucketDao.createDatasetBucketLink(datasetId, bucketResourceId);
        linkExists = datasetBucketDao.datasetBucketLinkExists(datasetId, bucketResourceId);
        assertTrue("Link should now exist.", linkExists);

        // create again - this should increment
        // count != 1, so this will fail - WHY?
        // How do we get it out of this state of count > 1?


        // decrement - link should still exist

        // decrement again - link should no longer exist

    }

    /*private UUID createDataset(DatasetRequestModel datasetRequest, String newName) throws Exception {
        datasetRequest.name(newName).defaultProfileId(billingProfile.getId());
        dataset = DatasetUtils.convertRequestWithGeneratedNames(datasetRequest);
        dataset.projectResourceId(projectId);
        String createFlightId = UUID.randomUUID().toString();
        UUID datasetId = UUID.randomUUID();
        dataset.id(datasetId);
        datasetDao.createAndLock(dataset, createFlightId);
        datasetDao.unlockExclusive(dataset.getId(), createFlightId);
        return datasetId;
    }

    private UUID createDataset(String datasetFile) throws Exception  {
        DatasetRequestModel datasetRequest = jsonLoader.loadObject(datasetFile, DatasetRequestModel.class);
        return createDataset(datasetRequest, datasetRequest.getName() + UUID.randomUUID().toString());
    }

    @Before
    public void setup() {
        BillingProfileRequestModel profileRequest = ProfileFixtures.randomBillingProfileRequest();
        billingProfile = profileDao.createBillingProfile(profileRequest, "hi@hi.hi");

        GoogleProjectResource projectResource = ResourceFixtures.randomProjectResource(billingProfile);
        projectId = resourceDao.createProject(projectResource);
    }

    @After
    public void teardown() {
        resourceDao.deleteProject(projectId);
        profileDao.deleteBillingProfileById(UUID.fromString(billingProfile.getId()));
    }

    @Test
    public void CompareBuckets() throws Exception {
        UUID dataset1 = createDataset("dataset-minimal.json");
        UUID dataset2 = createDataset("dataset-create-test.json");
        List<UUID> datasetIds = new ArrayList<>();
        datasetIds.add(dataset1);
        datasetIds.add(dataset2);
        //TODO Add bucket link
        UUID bulkIngestFlightId = UUID.randomUUID();
        //What project is this going to grab? Should I call a lower level call?
        /*GoogleBucketResource bucketForFile =
            resourceService.getOrCreateBucketForFile(
                dataset.getName(),
                billingProfile,
                bulkIngestFlightId.toString());


        // TODO Add manual calls to make links



        datasetDao.delete(dataset1);
        datasetDao.delete(dataset2);
    }*/
}
