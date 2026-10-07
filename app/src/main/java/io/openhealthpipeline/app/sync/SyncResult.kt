package io.openhealthpipeline.app.sync

import io.openhealthpipeline.app.health.model.SyncRecordResult
import io.openhealthpipeline.app.health.model.SyncRecordStatus

/**
 * Aggregated result of one complete sync run across all attempted record types.
 *
 * Used to update the UI and generate the INFO-level summary log entry.
 * Contains no raw health measurement values.
 */
data class SyncResult(

    /** System.currentTimeMillis() when the sync run started. */
    val startedAt: Long = System.currentTimeMillis(),

    /** System.currentTimeMillis() when the sync run completed. */
    val completedAt: Long = System.currentTimeMillis(),

    /** Per-type results. One entry per record type attempted. */
    val typeResults: List<SyncRecordResult> = emptyList()
) {

    /** Total records successfully read across all record types. */
    val totalRecordsRead: Int
        get() = typeResults.sumOf { it.recordsRead }

    /** Number of record types that completed with SUCCESS or PARTIAL status. */
    val succeededTypes: Int
        get() = typeResults.count {
            it.status == SyncRecordStatus.SUCCESS || it.status == SyncRecordStatus.PARTIAL
        }

    /**
     * Number of record types that encountered a hard error
     * (excludes PERMISSION_DENIED, which is a user decision not an error).
     */
    val failedTypes: Int
        get() = typeResults.count {
            it.status == SyncRecordStatus.HEALTH_CONNECT_ERROR ||
            it.status == SyncRecordStatus.MAPPING_ERROR
        }

    /** Number of record types skipped because the user denied the read permission. */
    val permissionDeniedTypes: Int
        get() = typeResults.count { it.status == SyncRecordStatus.PERMISSION_DENIED }

    /** True if at least one record type completed successfully with records. */
    val hasData: Boolean
        get() = totalRecordsRead > 0

    /** Duration of the sync run in milliseconds. */
    val durationMs: Long
        get() = completedAt - startedAt
}
