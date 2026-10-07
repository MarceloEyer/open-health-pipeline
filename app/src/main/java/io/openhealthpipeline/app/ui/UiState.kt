package io.openhealthpipeline.app.ui

import io.openhealthpipeline.app.health.HealthConnectAvailability
import io.openhealthpipeline.app.sync.SyncResult

/**
 * Complete immutable UI state for the main screen.
 *
 * Updated atomically by [MainViewModel]. The Compose UI observes [MainViewModel.uiState]
 * and recomposes only when this snapshot changes.
 *
 * Privacy constraint: no individual health measurement values are included here.
 * Only record counts, timestamps, status labels, and error classifications are exposed.
 */
data class UiState(

    /** Current Health Connect SDK availability as reported by the SDK. */
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,

    /** Whether the Steps read permission is currently granted. */
    val stepsPermissionGranted: Boolean = false,

    /** True while a sync run is in progress (disables the Sync Now button). */
    val isSyncing: Boolean = false,

    /**
     * Human-readable timestamp of the last successfully completed sync,
     * formatted as "yyyy-MM-dd HH:mm:ss". Null if no sync has completed.
     */
    val lastSyncAt: String? = null,

    /** Result object from the most recent sync run, or null before first sync. */
    val lastSyncResult: SyncResult? = null,

    /**
     * Non-sensitive error description from the most recent failed or partially
     * failed sync, or null if the last sync was clean. Cleared when a new
     * sync starts.
     */
    val lastError: String? = null
)
