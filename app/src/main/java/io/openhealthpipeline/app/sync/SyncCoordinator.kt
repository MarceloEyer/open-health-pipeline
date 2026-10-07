package io.openhealthpipeline.app.sync

import android.util.Log
import io.openhealthpipeline.app.health.HealthConnectAvailability
import io.openhealthpipeline.app.health.HealthConnectRepository
import io.openhealthpipeline.app.health.HealthPermissions
import io.openhealthpipeline.app.health.HealthRecordReader
import io.openhealthpipeline.app.health.model.ErrorCategory
import io.openhealthpipeline.app.health.model.RawHealthRecord
import io.openhealthpipeline.app.health.model.SyncRecordResult
import io.openhealthpipeline.app.health.model.SyncRecordStatus

private const val TAG = "SyncCoordinator"

/**
 * Orchestrates one complete sync run.
 *
 * Design:
 *  - Each record type is processed independently; one failure does not stop others.
 *  - The [onPage] callback is called for each page of records as they arrive,
 *    enabling streaming persistence in Stage C without accumulating full history.
 *  - For Stage A, [onPage] is used only for in-memory counting; no persistence yet.
 *  - Logs start/end summary at INFO level without health values.
 *  - Returns a structured [SyncResult] for the UI.
 *
 * Stage B will expand the record type list; the coordinator loops generically.
 * Stage C will introduce a HealthRecordSink passed to this coordinator.
 */
class SyncCoordinator(private val repository: HealthConnectRepository) {

    /**
     * Runs one complete sync run.
     *
     * Checks availability and permissions before attempting each type.
     * Returns immediately with an empty result if Health Connect is not available.
     *
     * @param onPage Called for each page of mapped records from any record type.
     *               Parameters: (recordType: String, page: List<RawHealthRecord>).
     *               For Stage A, callers may ignore or accumulate these.
     *               For Stage C, callers should persist each page before returning.
     * @return [SyncResult] with per-type outcomes and aggregate counts.
     */
    suspend fun runSync(
        onPage: suspend (recordType: String, page: List<RawHealthRecord>) -> Unit = { _, _ -> }
    ): SyncResult {
        val startedAt = System.currentTimeMillis()
        Log.i(TAG, "Sync started")

        // Guard: Health Connect must be available
        if (repository.availability.value != HealthConnectAvailability.AVAILABLE) {
            Log.w(TAG, "Sync aborted: Health Connect not available (${repository.availability.value})")
            return SyncResult(
                startedAt = startedAt,
                completedAt = System.currentTimeMillis()
            )
        }

        val client = repository.client
        if (client == null) {
            Log.w(TAG, "Sync aborted: HealthConnectClient is null despite AVAILABLE state")
            return SyncResult(
                startedAt = startedAt,
                completedAt = System.currentTimeMillis()
            )
        }

        // Query granted permissions once for the entire run
        val grantedPermissions = repository.getGrantedPermissions()
        Log.d(TAG, "Granted permissions count: ${grantedPermissions.size}")

        val results = mutableListOf<SyncRecordResult>()
        val reader = HealthRecordReader(client)

        // -----------------------------------------------------------------------
        // Stage A: Steps only.
        // Stage B will replace this with a loop over a RecordTypeRegistry.
        // -----------------------------------------------------------------------
        if (!HealthPermissions.hasMinimumPermissions(grantedPermissions)) {
            Log.w(TAG, "Steps permission not granted — skipping StepsRecord")
            results.add(
                SyncRecordResult(
                    recordType = "StepsRecord",
                    status = SyncRecordStatus.PERMISSION_DENIED,
                    errorCategory = ErrorCategory.PERMISSION_DENIED,
                    errorMessage = "Steps read permission not granted"
                )
            )
        } else {
            val stepsResult = reader.readSteps { page ->
                onPage("StepsRecord", page)
            }
            results.add(stepsResult)
        }

        val completedAt = System.currentTimeMillis()
        val syncResult = SyncResult(
            startedAt = startedAt,
            completedAt = completedAt,
            typeResults = results
        )

        Log.i(
            TAG,
            "Sync completed in ${syncResult.durationMs}ms — " +
            "attempted: ${results.size}, " +
            "succeeded: ${syncResult.succeededTypes}, " +
            "failed: ${syncResult.failedTypes}, " +
            "permission denied: ${syncResult.permissionDeniedTypes}, " +
            "total records read: ${syncResult.totalRecordsRead}"
        )

        return syncResult
    }
}