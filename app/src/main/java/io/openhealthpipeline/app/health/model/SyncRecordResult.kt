package io.openhealthpipeline.app.health.model

/**
 * Result of reading one record type during a sync run.
 *
 * Surfaces per-type success or failure to the UI and diagnostic logs
 * without including any raw health measurement values.
 *
 * One instance is produced per record type attempted in a sync run.
 */
data class SyncRecordResult(

    /** Unqualified record class name, e.g. "StepsRecord". */
    val recordType: String,

    /** Outcome of the read attempt for this record type. */
    val status: SyncRecordStatus,

    /** Number of records successfully mapped to [RawHealthRecord]. */
    val recordsRead: Int = 0,

    /** Number of paginated pages consumed (useful for diagnostics). */
    val pagesRead: Int = 0,

    /** High-level error classification, or null on success. */
    val errorCategory: ErrorCategory? = null,

    /**
     * Short non-sensitive error description, or null on success.
     * Must NOT contain health values, auth tokens, or full stack traces.
     */
    val errorMessage: String? = null
)

/**
 * High-level outcome for one record type during a sync run.
 */
enum class SyncRecordStatus {
    /** All pages read and all records mapped successfully. */
    SUCCESS,

    /** Read permission for this type was not granted by the user. */
    PERMISSION_DENIED,

    /** The record type is not available on this device/SDK/provider version. */
    FEATURE_UNAVAILABLE,

    /** Health Connect returned an error during reading. */
    HEALTH_CONNECT_ERROR,

    /** Records were read but mapping to [RawHealthRecord] failed. */
    MAPPING_ERROR,

    /** Some pages succeeded and some failed. */
    PARTIAL
}

/**
 * Categorical classification of errors for structured logging and UI display.
 * Deliberately coarse-grained — detailed exception types go in errorMessage.
 */
enum class ErrorCategory {
    PERMISSION_DENIED,
    SECURITY_EXCEPTION,
    REMOTE_EXCEPTION,
    FEATURE_UNAVAILABLE,
    MAPPING_FAILURE,
    UNKNOWN
}
