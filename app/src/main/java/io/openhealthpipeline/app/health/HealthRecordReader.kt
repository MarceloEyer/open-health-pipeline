package io.openhealthpipeline.app.health

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.openhealthpipeline.app.health.model.ErrorCategory
import io.openhealthpipeline.app.health.model.RawHealthRecord
import io.openhealthpipeline.app.health.model.SyncRecordResult
import io.openhealthpipeline.app.health.model.SyncRecordStatus
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.format.DateTimeFormatter

private const val TAG = "HealthRecordReader"

/**
 * Page size for [HealthConnectClient.readRecords] requests.
 *
 * 1000 is the practical maximum supported by Health Connect. This is an
 * implementation parameter per the design; it can be tuned without changing
 * any product requirement.
 */
private const val PAGE_SIZE = 1000

/**
 * Historical window in days to use when READ_HEALTH_DATA_HISTORY permission
 * has not been granted. Stays within Health Connect's default access window.
 */
private const val DEFAULT_HISTORY_DAYS = 30L

/**
 * Reads raw Health Connect records with full pagination support.
 *
 * Design constraints:
 *  - Reads one page at a time (never accumulates full history in memory).
 *  - Calls [onPage] for each page so callers can process/persist incrementally
 *    (streaming pattern; important for Stage C Supabase persistence).
 *  - Each record type has its own reader method for type safety.
 *  - Logs record counts at DEBUG level; never logs measurement values.
 *  - Returns a structured [SyncRecordResult] regardless of outcome.
 */
class HealthRecordReader(private val client: HealthConnectClient) {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    // -------------------------------------------------------------------------
    // Public reader: StepsRecord
    // -------------------------------------------------------------------------

    /**
     * Reads all [StepsRecord] entries available within the allowed time range,
     * using a 30-day window (safe without extended history permission).
     *
     * Stage B will introduce READ_HEALTH_DATA_HISTORY permission and expand
     * the historical window to the maximum allowed by Health Connect.
     *
     * @param onPage Called with each mapped page as it arrives. Allows the
     *               caller to persist or process records before the next page
     *               is fetched, keeping memory usage bounded.
     * @return [SyncRecordResult] summarising the read operation.
     */
    suspend fun readSteps(
        onPage: suspend (List<RawHealthRecord>) -> Unit = {}
    ): SyncRecordResult {
        val recordType = "StepsRecord"
        Log.i(TAG, "Starting read: $recordType")

        val endTime = Instant.now()
        val startTime = endTime.minusSeconds(DEFAULT_HISTORY_DAYS * 24L * 60L * 60L)
        Log.d(TAG, "$recordType time range: ${isoFormatter.format(startTime)} to ${isoFormatter.format(endTime)}")

        return readStepsPaginated(
            timeRange = TimeRangeFilter.between(startTime, endTime),
            onPage = onPage
        )
    }

    // -------------------------------------------------------------------------
    // StepsRecord paginated reader (type-specific, avoids unsafe generic casts)
    // -------------------------------------------------------------------------

    private suspend fun readStepsPaginated(
        timeRange: TimeRangeFilter,
        onPage: suspend (List<RawHealthRecord>) -> Unit
    ): SyncRecordResult {
        val recordType = "StepsRecord"
        var totalRecords = 0
        var totalPages = 0
        var pageToken: String? = null

        return try {
            do {
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRange,
                    pageSize = PAGE_SIZE,
                    pageToken = pageToken
                )

                val response = client.readRecords(request)
                totalPages++

                val pageCount = response.records.size
                Log.d(TAG, "Page $totalPages of $recordType: $pageCount records")

                val mapped: List<RawHealthRecord> = response.records.map { record ->
                    mapStepsRecord(record)
                }
                totalRecords += mapped.size

                if (mapped.isNotEmpty()) {
                    onPage(mapped)
                }

                pageToken = response.pageToken
            } while (pageToken != null)

            Log.i(TAG, "Completed read of $recordType: $totalRecords records across $totalPages pages")
            SyncRecordResult(
                recordType = recordType,
                status = SyncRecordStatus.SUCCESS,
                recordsRead = totalRecords,
                pagesRead = totalPages
            )
        } catch (e: SecurityException) {
            // Thrown when the read permission has been revoked mid-sync
            Log.w(TAG, "Permission denied reading $recordType: ${e.javaClass.simpleName}")
            SyncRecordResult(
                recordType = recordType,
                status = SyncRecordStatus.PERMISSION_DENIED,
                recordsRead = totalRecords,
                pagesRead = totalPages,
                errorCategory = ErrorCategory.PERMISSION_DENIED,
                errorMessage = e.javaClass.simpleName
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Covers RemoteException, IOException, and any other Health Connect error
            Log.e(TAG, "Error reading $recordType [${e.javaClass.simpleName}]: ${e.message}")
            SyncRecordResult(
                recordType = recordType,
                status = SyncRecordStatus.HEALTH_CONNECT_ERROR,
                recordsRead = totalRecords,
                pagesRead = totalPages,
                errorCategory = ErrorCategory.REMOTE_EXCEPTION,
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Mapper: StepsRecord -> RawHealthRecord
    // -------------------------------------------------------------------------

    /**
     * Maps a [StepsRecord] to the lossless [RawHealthRecord] representation.
     *
     * All metadata fields exposed by Health Connect are preserved.
     * The step count is stored in payload["count"].
     *
     * The step count VALUE itself is deliberately not logged anywhere in this method.
     */
    private fun mapStepsRecord(record: StepsRecord): RawHealthRecord {
        val meta = record.metadata
        // lastModifiedTime is non-null in the Health Connect SDK (Instant type)
        val lastModified = try {
            isoFormatter.format(meta.lastModifiedTime)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return RawHealthRecord(
            hcRecordId = meta.id,
            recordType = "StepsRecord",
            startTime = isoFormatter.format(record.startTime),
            endTime = isoFormatter.format(record.endTime),
            startZoneOffset = record.startZoneOffset?.toString(),
            endZoneOffset = record.endZoneOffset?.toString(),
            dataOrigin = meta.dataOrigin.packageName.takeIf { it.isNotEmpty() },
            lastModifiedTime = lastModified,
            clientRecordId = meta.clientRecordId,
            clientRecordVersion = meta.clientRecordVersion.takeIf { it != 0L },
            deviceType = meta.device?.type,
            deviceManufacturer = meta.device?.manufacturer,
            deviceModel = meta.device?.model,
            payload = mapOf("count" to record.count)
        )
    }
}
