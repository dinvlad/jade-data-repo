package scripts.testscripts;

import bio.terra.datarepo.api.ResourcesApi;
import bio.terra.datarepo.client.ApiClient;
import bio.terra.datarepo.model.EnumerateBillingProfileModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.TestUserSpecification;
import scripts.utils.DataRepoUtils;

public class EnumerateProfiles extends runner.TestScript {

  private static final Logger logger = LoggerFactory.getLogger(EnumerateProfiles.class);

  /** Public constructor so that this class can be instantiated via reflection. */
  public EnumerateProfiles() {
    super();
  }

  public void userJourney(TestUserSpecification testUser) throws Exception {
    try {
      ApiClient apiClient = DataRepoUtils.getClientForTestUser(testUser, server);
      ResourcesApi resourcesApi = new ResourcesApi(apiClient);
      EnumerateBillingProfileModel profiles = resourcesApi.enumerateProfiles(0, 10);

      int httpStatus = resourcesApi.getApiClient().getStatusCode();
      logger.debug(
          "Enumerate profiles: HTTP status {}, number of profiles found = {}",
          httpStatus,
          profiles.getTotal());
    } catch (Exception e) {
      logger.info("User journey failed: ", e);
      throw e;
    }
  }
}
