package scripts.testscripts.baseclasses;

import bio.terra.datarepo.api.ResourcesApi;
import bio.terra.datarepo.client.ApiClient;
import bio.terra.datarepo.model.BillingProfileModel;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.TestUserSpecification;
import scripts.utils.DataRepoUtils;
import scripts.utils.SAMUtils;

public class CreateProfile extends runner.TestScript {
  private static final Logger logger = LoggerFactory.getLogger(CreateProfile.class);

  protected TestUserSpecification profileCreator;
  protected BillingProfileModel billingProfileModel;

  public void setup(List<TestUserSpecification> testUsers) throws Exception {
    // pick the a user that is a Data Repo steward to be the profile creator
    profileCreator = SAMUtils.findTestUserThatIsDataRepoSteward(testUsers, server);
    if (profileCreator == null) {
      throw new IllegalArgumentException("None of the test users are Data Repo stewards.");
    }
    logger.debug("profileCreator: {}", profileCreator.name);

    // get the ApiClient for the profile creator
    ApiClient profileCreatorClient = DataRepoUtils.getClientForTestUser(profileCreator, server);
    ResourcesApi resourcesApi = new ResourcesApi(profileCreatorClient);

    // create a new profile
    billingProfileModel =
        DataRepoUtils.createProfile(resourcesApi, billingAccount, "profile-simple", true);
    logger.info("Successfully created profile: {}", billingProfileModel.getProfileName());
  }

  public void cleanup(List<TestUserSpecification> testUsers) throws Exception {
    // get the ApiClient for the profile creator
    ApiClient profileCreatorClient = DataRepoUtils.getClientForTestUser(profileCreator, server);
    ResourcesApi resourcesApi = new ResourcesApi(profileCreatorClient);

    // delete the profile
    resourcesApi.deleteProfile(billingProfileModel.getId());
    logger.info("Successfully deleted profile: {}", billingProfileModel.getProfileName());
  }
}
