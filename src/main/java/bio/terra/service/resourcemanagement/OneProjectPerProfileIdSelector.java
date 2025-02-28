package bio.terra.service.resourcemanagement;

import bio.terra.model.BillingProfileModel;
import bio.terra.service.resourcemanagement.google.GoogleResourceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"terra", "google"})
public class OneProjectPerProfileIdSelector implements DataLocationSelector {
    private final GoogleResourceConfiguration resourceConfiguration;

    @Autowired
    public OneProjectPerProfileIdSelector(GoogleResourceConfiguration resourceConfiguration) {
        this.resourceConfiguration = resourceConfiguration;
    }

    @Override
        public String projectIdForDataset(String datasetName, BillingProfileModel billingProfile) {
        return getSuffixForProfileId(billingProfile);
    }

    @Override
    public String projectIdForSnapshot(String snapshotName, BillingProfileModel billingProfile) {
        return getSuffixForProfileId(billingProfile);
    }

    @Override
    public String projectIdForFile(String datasetName, BillingProfileModel billingProfile) {
        return getSuffixForProfileId(billingProfile);
    }

    @Override
    public String bucketForFile(String datasetName, BillingProfileModel billingProfile) {
        return projectIdForFile(datasetName, billingProfile) + "-bucket";
    }

    private String getSuffixForProfileId(BillingProfileModel billingProfile) {
        String lowercaseProfileName = billingProfile.getProfileName().toLowerCase();
        String profileSuffix = "-" + lowercaseProfileName.replaceAll("[^a-z-0-9]", "-");
        // The project id below is the name of the core project
        return resourceConfiguration.getProjectId() + profileSuffix;
    }
}
