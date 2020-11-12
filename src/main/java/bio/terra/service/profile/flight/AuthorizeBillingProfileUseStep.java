package bio.terra.service.profile.flight;

import bio.terra.model.BillingProfileModel;
import bio.terra.service.iam.AuthenticatedUserRequest;
import bio.terra.service.profile.ProfileService;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * This step is intended to be shared by all flights that are allocating new resources
 * within a billing profile. The step ensures that the caller has proper access to the
 * billing profile and that the reference billing account remains enabled and accessible.
 *
 * It takes the profile id as input. On successful authorization, the associated
 * billing profile is stored in the working map of the flight in the ProfileMapKeys.PROFILE_MODEL
 * entry. On failure, exception is thrown and the flight will fail.
 */
public class AuthorizeBillingProfileUseStep implements Step {
    private final ProfileService profileService;
    private final UUID profileId;
    private final AuthenticatedUserRequest user;
    private static Logger logger = LoggerFactory.getLogger(AuthorizeBillingProfileUseStep.class);

    public AuthorizeBillingProfileUseStep(ProfileService profileService,
                                          String profileId,
                                          AuthenticatedUserRequest user) {
        this.profileService = profileService;
        this.profileId = UUID.fromString(profileId);
        this.user = user;
    }

    @Override
    public StepResult doStep(FlightContext context) throws InterruptedException {
        logger.info("[BUCKET_TESTING]: Profile id: {}; This profile id should match the profile id that" +
                "was the result from the CreateProfileFlight", profileId);
        BillingProfileModel profileModel = profileService.authorizeLinking(profileId, user);
        logger.info("[BUCKET_TESTING]: Profile Model - Should match the profile that" +
            "was the result from the CreateProfileFlight", profileModel.toString());
        FlightMap workingMap = context.getWorkingMap();
        workingMap.put(ProfileMapKeys.PROFILE_MODEL, profileModel);
        return StepResult.getStepResultSuccess();
    }

    @Override
    public StepResult undoStep(FlightContext context) throws InterruptedException {
        // This step has no side effects
        return StepResult.getStepResultSuccess();
    }
}
