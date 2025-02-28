package bio.terra.service.filedata.flight.ingest;

import bio.terra.app.configuration.ApplicationConfiguration;
import bio.terra.service.configuration.ConfigurationService;
import bio.terra.service.dataset.Dataset;
import bio.terra.service.dataset.DatasetService;
import bio.terra.service.filedata.FileService;
import bio.terra.service.filedata.google.firestore.FireStoreDao;
import bio.terra.service.filedata.google.firestore.FireStoreUtils;
import bio.terra.service.filedata.google.gcs.GcsPdao;
import bio.terra.service.job.JobMapKeys;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.RetryRuleRandomBackoff;
import org.springframework.context.ApplicationContext;

import java.util.UUID;

import static bio.terra.common.FlightUtils.getDefaultRandomBackoffRetryRule;

/*
 * The flight is launched from the IngestDriverStep within one of the bulk load flights.
 * It performs a single file ingest into a dataset.
 * Input parameters expected:
 * - DATASET_ID - dataset into which we load the file
 * - REQUEST - a FileLoadModel describing the file to load
*/

public class FileIngestWorkerFlight extends Flight {

    public FileIngestWorkerFlight(FlightMap inputParameters,
                                  Object applicationContext) {
        super(inputParameters, applicationContext);

        ApplicationContext appContext = (ApplicationContext) applicationContext;
        FireStoreDao fileDao = (FireStoreDao)appContext.getBean("fireStoreDao");
        FireStoreUtils fireStoreUtils = (FireStoreUtils)appContext.getBean("fireStoreUtils");
        FileService fileService = (FileService)appContext.getBean("fileService");
        GcsPdao gcsPdao = (GcsPdao)appContext.getBean("gcsPdao");
        DatasetService datasetService = (DatasetService)appContext.getBean("datasetService");
        ApplicationConfiguration appConfig =
            (ApplicationConfiguration)appContext.getBean("applicationConfiguration");
        ConfigurationService configService = (ConfigurationService)appContext.getBean("configurationService");

        UUID datasetId = UUID.fromString(inputParameters.get(JobMapKeys.DATASET_ID.getKeyName(), String.class));
        Dataset dataset = datasetService.retrieve(datasetId);

        RetryRuleRandomBackoff fileSystemRetry = getDefaultRandomBackoffRetryRule(appConfig.getMaxStairwayThreads());

        // The flight plan:
        // 1. Generate the new file id and store it in the working map. We need to allocate the file id before any
        //    other operation so that it is persisted in the working map. In particular, IngestFileDirectoryStep undo
        //    needs to know the file id in order to clean up.
        // 2. Create the directory entry for the file. The state where there is a directory entry for a file, but
        //    no entry in the file collection, indicates that the file is being ingested (or deleted) and so REST API
        //    lookups will not reveal that it exists. We make the directory entry first, because that atomic operation
        //    prevents a second ingest with the same path from getting created.
        // 3. Copy the file into the bucket. Return the gspath, checksum, size, and create time in the working map.
        // 4. Create the file entry in the filesystem. The file object takes the gspath, checksum, size, and create
        //    time of the actual file in GCS. That ensures that the file info we return on REST API (and DRS) lookups
        //    matches what users will see when they examine the GCS object. When the file entry is (atomically)
        //    created in the file firestore collection, the file becomes visible for REST API lookups.
        addStep(new IngestFileIdStep(configService));
        addStep(new IngestFileDirectoryStep(fileDao, fireStoreUtils, dataset), fileSystemRetry);
        addStep(new IngestFilePrimaryDataStep(dataset, gcsPdao, configService));
        addStep(new IngestFileFileStep(fileDao, fileService, dataset), fileSystemRetry);
    }

}
