package bio.terra.service.configuration;

import bio.terra.service.configuration.exception.ConfigNotFoundException;
import org.apache.commons.lang3.StringUtils;


/**
 * NOTE: the string form of the enumerations are used in tests. A simple IntelliJ rename will not work properly.
 */
// TODO: when we move to OpenAPI V3, we can put this an enum in the swagger and use the enums in the caller
public enum ConfigEnum {
    // -- parameters --
    SAM_RETRY_INITIAL_WAIT_SECONDS,
    SAM_RETRY_MAXIMUM_WAIT_SECONDS,
    SAM_OPERATION_TIMEOUT_SECONDS,
    LOAD_BULK_ARRAY_FILES_MAX,
    LOAD_BULK_FILES_MAX,
    LOAD_CONCURRENT_FILES,
    LOAD_CONCURRENT_INGESTS,
    LOAD_DRIVER_WAIT_SECONDS,
    LOAD_HISTORY_COPY_CHUNK_SIZE,
    LOAD_HISTORY_WAIT_SECONDS,
    FIRESTORE_SNAPSHOT_BATCH_SIZE,
    FIRESTORE_SNAPSHOT_CACHE_SIZE,
    FIRESTORE_VALIDATE_BATCH_SIZE,
    FIRESTORE_QUERY_BATCH_SIZE,
    AUTH_CACHE_TIMEOUT_SECONDS,
    AUTH_CACHE_SIZE,
    ALLOW_REUSE_EXISTING_BUCKETS,

    // -- faults --
    SAM_TIMEOUT_FAULT,
    CREATE_ASSET_FAULT,
    // TODO: When we do DR-737 and attach data to faults, these two can be combined into one.
    LOAD_LOCK_CONFLICT_STOP_FAULT,
    LOAD_LOCK_CONFLICT_CONTINUE_FAULT,
    LOAD_SKIP_FILE_LOAD,

    BUCKET_LOCK_CONFLICT_STOP_FAULT,
    BUCKET_LOCK_CONFLICT_CONTINUE_FAULT,

    DATASET_DELETE_LOCK_CONFLICT_STOP_FAULT,
    DATASET_DELETE_LOCK_CONFLICT_CONTINUE_FAULT,

    SNAPSHOT_DELETE_LOCK_CONFLICT_STOP_FAULT,
    SNAPSHOT_DELETE_LOCK_CONFLICT_CONTINUE_FAULT,

    FILE_INGEST_LOCK_CONFLICT_STOP_FAULT,
    FILE_INGEST_LOCK_CONFLICT_CONTINUE_FAULT,

    FILE_INGEST_LOCK_RETRY_FAULT,
    FILE_INGEST_LOCK_FATAL_FAULT,
    FILE_INGEST_UNLOCK_RETRY_FAULT,
    FILE_INGEST_UNLOCK_FATAL_FAULT,

    FILE_DELETE_LOCK_CONFLICT_STOP_FAULT,
    FILE_DELETE_LOCK_CONFLICT_CONTINUE_FAULT,

    TABLE_INGEST_LOCK_CONFLICT_STOP_FAULT,
    TABLE_INGEST_LOCK_CONFLICT_CONTINUE_FAULT,

    SOFT_DELETE_LOCK_CONFLICT_STOP_FAULT,
    SOFT_DELETE_LOCK_CONFLICT_CONTINUE_FAULT,

    DATASET_GRANT_ACCESS_FAULT,
    SNAPSHOT_GRANT_ACCESS_FAULT,
    SNAPSHOT_GRANT_FILE_ACCESS_FAULT,

    FIRESTORE_RETRIEVE_FAULT,

    LIVENESS_FAULT,
    CRITICAL_SYSTEM_FAULT,

    // Faults to test the fault system
    UNIT_TEST_SIMPLE_FAULT,
    UNIT_TEST_COUNTED_FAULT;

    public static ConfigEnum lookupByApiName(String apiName) {
        for (ConfigEnum config : values()) {
            if (StringUtils.equalsIgnoreCase(config.name(), apiName)) {
                return config;
            }
        }
        throw new ConfigNotFoundException("Configuration '" + apiName + "' was not found");
    }
}
